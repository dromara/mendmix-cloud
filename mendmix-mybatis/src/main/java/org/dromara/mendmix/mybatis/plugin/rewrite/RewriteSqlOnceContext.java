package org.dromara.mendmix.mybatis.plugin.rewrite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.model.AuthUser;
import org.dromara.mendmix.common.model.DataPermItem;
import org.dromara.mendmix.common.model.PageParams;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.mybatis.DeptPermType;
import org.dromara.mendmix.mybatis.MybatisConfigs;
import org.dromara.mendmix.mybatis.MybatisRuntimeContext;
import org.dromara.mendmix.mybatis.metadata.MetadataHelper;
import org.dromara.mendmix.mybatis.plugin.OnceContextVal;
import org.dromara.mendmix.mybatis.plugin.rewrite.annotation.DataPermissionItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SubSelect;

/**
 * 
 * <br>
 * @author jiangwei
 * @version 1.0.0
 * @date 2023年4月23日
 */
public class RewriteSqlOnceContext {
	
	private final static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.mybatis");
	
	OnceContextVal invocation;
	DataPermissionStrategy strategy;
	private Map<String, String[]> dataPermValues;
	boolean handleDataPerm;
	boolean handleTenant;
	boolean handleSoftDelete;
	boolean handleOrderBy;
	
	String currentGroupKey;
	
	List<RewriteTable> rewriteTables;
	
	AuthUser currentUser;
	String currentTenantId;
	
	boolean unionSelect;
	
	boolean loadedCurrentGroupPermData;
	boolean mainTableHandledTenant; //主表已处理租户
	
	boolean traceLogging = CurrentRuntimeContext.isDebugMode();

	public RewriteSqlOnceContext(OnceContextVal invocation,boolean isFieldTenantMode ,boolean dynaDataPermEnaled) {
		currentUser = CurrentRuntimeContext.getCurrentUser();
		this.invocation = invocation;
		this.handleDataPerm = dynaDataPermEnaled 
				&& (currentUser == null || !currentUser.isAdmin())
				&& !MybatisRuntimeContext.isIgnoreDataPermission() 
				&& (strategy = MybatisRuntimeContext.getDataPermissionStrategy()) != null;
		this.handleTenant = isFieldTenantMode 
				&& !MybatisRuntimeContext.isIgnoreTenantMode() 
				&& !MybatisConfigs.ignoreTenant(invocation.getMapperNameSpace());
		PageParams pageParam = invocation.getPageObject();
		this.handleOrderBy = pageParam != null && pageParam.getOrderBys() != null && !pageParam.getOrderBys().isEmpty();
		if(handleDataPerm && strategy == null) {
			strategy = MybatisRuntimeContext.getDataPermissionStrategy();
		}
		if(isFieldTenantMode) {
			currentTenantId = CurrentRuntimeContext.getTenantId();
		}
		if(traceLogging && !this.handleDataPerm) {
			logger.info("<trace_logging> handleDataPerm = false\n -currentUser:{}\n -ignoreDataPermission:{}\n -withStrategy:{}"
					,currentUser == null ? null : (currentUser.isAdmin()? "isAdmin" : "isNotAdmin")
					,MybatisRuntimeContext.isIgnoreDataPermission()
					,MybatisRuntimeContext.getDataPermissionStrategy() != null);
		}
	}
	
	
	public Map<String, String[]> getDataPermValues() {
		if(dataPermValues == null && handleDataPerm) {
			dataPermValues = MybatisRuntimeContext.getDataPermissionValues(currentGroupKey);
		}
		if(dataPermValues == null) {
			dataPermValues = new HashMap<>(3);
			if(handleDataPerm)MybatisRuntimeContext.setDataPermissionValues(dataPermValues, false);
		}
		return dataPermValues;
	}

    public boolean withTableShardingRule() {
    	return invocation.getTableNameMapping() != null && !invocation.getTableNameMapping().isEmpty();
    }

	public boolean withConditionReriteRule() {
		return handleDataPerm 
			   || handleTenant 
			   || handleSoftDelete 
			   || handleOrderBy;
	}

	
	public String[] getPermValues(String field) {
		return getDataPermValues().get(field);
	}
	
	public void loadDataPermValues(SqlRewriteHandler handler) {
		if(!handleDataPerm || loadedCurrentGroupPermData)return;
		final boolean withGroup = StringUtils.isNotBlank(currentGroupKey);
		try {
			if(withGroup) {
				if(traceLogging)logger.info("<trace_logging> reloadGroupDataPermValues:{}",currentGroupKey);
				mainTableHandledTenant = false;
				//当前部门
				if(currentGroupKey.startsWith(OrgPermissionHelper.DEFAULT_POSITION_GROUP_NAME)) {
					String positionId = currentGroupKey.substring(currentGroupKey.indexOf(GlobalConstants.COLON) + 1);
					ThreadLocalContext.set(OrgPermissionHelper.CONTEXT_CURRENT_POSTION_ID, positionId);				
				}
				dataPermValues = null;
				MybatisRuntimeContext.setDataPermissionValues(null, false);
			}
			dataPermValues = getDataPermValues();
			
			//是否已预处理部门权限数据
    		boolean preparedOrgPermData = dataPermValues.containsKey(handler.getDeptPropName());
			if(rewriteTables != null) {
				//加载部门数据
				if(handler.getDeptPropName() != null 
						&& dataPermValues.containsKey(MybatisConfigs.ORG_DATA_PERM_NAME)) {
					if(rewriteTables.stream().anyMatch(
			    			o -> o.getRewriteColumnMapping().containsKey(handler.getDeptPropName()))
			    	) {
						OrgPermissionHelper.prepareOrganizationPermission(invocation, dataPermValues, handler.getDeptPropName());
			    		preparedOrgPermData = true;
			    		if(withGroup) {
			    			if(traceLogging)logger.info("<trace_logging> reloadPrepareOrganizationPermission for permGroup:{}",currentGroupKey);
			    			//由于第一次初始化的原因，这个不能放finally
				    		ThreadLocalContext.remove(OrgPermissionHelper.CONTEXT_CURRENT_POSTION_ID);
			    		}
			    	}
				}
				//
				for (RewriteTable rewriteTable : rewriteTables) {
					setTableOwnerColumns(handler, rewriteTable);
					//使用启用多表组织权限统一分组处理
	    			if(!withGroup && preparedOrgPermData && rewriteTables.size() > 1) {				
	    				rewriteTable.setUsingGlobalOrgPerm(isUsingGroupOrgPerm(handler, rewriteTable));
	    			}
				}
			}
			if(traceLogging) {
				logger.info(this.toLogString());
				logger.info("<trace_logging> loadDataPermValues\n -group:{}\n -values:{}",currentGroupKey,JsonUtils.toJson(getDataPermValues()));
			}else if(logger.isDebugEnabled()) {
				logger.debug(this.toLogString());
			}
			loadedCurrentGroupPermData = true;
		} finally {
			if(withGroup)ThreadLocalContext.remove(CustomRequestHeaders.HEADER_REFERER_PERM_GROUP);
		}
	}
	
	public boolean isHandleGroupOrgPermission() {
		if(rewriteTables.size() <= 1)return false;
		boolean has = rewriteTables.stream().anyMatch(o -> o.isUsingGlobalOrgPerm());
		if(!has)return has;
		String[] values = getDataPermValues().get(MybatisConfigs.ORG_DATA_PERM_NAME);
		if(values == null)return false;
		for (String val : values) {
			if(has = !DeptPermType.none.name().equals(val)) {
				return has;
			}
		}
		return false;
	}
	
	public List<String> getPermGroupKeys(){
		if(!handleDataPerm || !MybatisConfigs.DATA_PERM_USING_GROUP_MODE) {
			return null;
		}
		//前端传入
		String group = CurrentRuntimeContext.getContextVal(CustomRequestHeaders.HEADER_REFERER_PERM_GROUP, false);
		if(StringUtils.isNotBlank(group)) {
			return Arrays.asList(StringUtils.split(group, GlobalConstants.COMMA));
		}
		List<DataPermItem> permssions = MybatisRuntimeContext.userPermissionProvider().findCurrentAllPermissions();
		if(permssions == null || permssions.isEmpty())return null;
		List<String> groupKeys = permssions.stream().filter(
			o -> o.getGroupName() != null
		 ).map(
			o -> o.getGroupName()
		 ).distinct().collect(Collectors.toList());
		//是否某个分组全部all权限
		if(groupKeys.size() > 1) {
			boolean withAllPerm;
			for (String groupKey : groupKeys) {
				withAllPerm = permssions.stream().filter(
				      o -> groupKey.equals(o.getGroupName())
				).allMatch(o -> (o.isAll() || (o.getValues() != null && o.getValues().contains(DeptPermType._ALL_.name()))) );
			   if(withAllPerm) {
				   if(traceLogging) {
						logger.info("<trace_logging> permissionGroup:{} with AllPermission!!!",groupKey);
				   }
				   return Arrays.asList(DeptPermType._ALL_.name());
			   }
			}
		}
		
		if(traceLogging && !groupKeys.isEmpty()) {
			logger.info("<trace_logging> resolve permissionGroups:{}",groupKeys);
		}
		return groupKeys;
	}
	
	public void parseRewriteTable(SqlRewriteHandler handler,PlainSelect select) {
		if(rewriteTables == null) {
			rewriteTables = new ArrayList<>();
		}
		Table table;
		RewriteTable rewriteTable;
		FromItem fromItem = select.getFromItem();
		if(fromItem instanceof Table) {
			table = (Table) select.getFromItem();
			rewriteTable = buildRewriteTable(handler,table, false);
			if(rewriteTable.getRewritedTableName() != null) {
				select.setFromItem(rewriteTable.getTable());
			}
			rewriteTable.setAppendConditonTo(select);
		}else if(fromItem instanceof SubSelect) {
			SelectBody selectBody = ((SubSelect)fromItem).getSelectBody();
			if(selectBody instanceof PlainSelect) {
				parseRewriteTable(handler, (PlainSelect)selectBody);
			}
		}
		
		List<Join> joins = select.getJoins();
		if(joins != null) {
			for (Join join : joins) {
				if(join.getRightItem() instanceof Table) {
					table = (Table) join.getRightItem();
					rewriteTable = buildRewriteTable(handler,table, true);
					if(rewriteTable.getRewritedTableName() != null) {
						join.setRightItem(rewriteTable.getTable());
					}
					//
					boolean appendConditonUsingOn = join.getOnExpressions().size() > 0;
					if(appendConditonUsingOn) {
						if(strategy != null) {
							appendConditonUsingOn = strategy.isJoinConditionWithOn();
						}else if(join.isInner()){
							appendConditonUsingOn = MybatisConfigs.DATA_PERM_INNER_JOIN_USING_ON;
						}
					}
					rewriteTable.setAppendConditonUsingOn(appendConditonUsingOn);
					if(appendConditonUsingOn) {
						rewriteTable.setAppendConditonTo(join);
					}else {
						rewriteTable.setAppendConditonTo(select);
					}
				}else if(join.getRightItem() instanceof SubSelect) {
					SelectBody selectBody = ((SubSelect)join.getRightItem()).getSelectBody();
					if(selectBody instanceof PlainSelect) {
						parseRewriteTable(handler, (PlainSelect)selectBody);
					}
				}
			}
		}
	}
	
	
	private RewriteTable buildRewriteTable(SqlRewriteHandler handler,Table table,boolean joinTable) {
		RewriteTable rewriteTable = new RewriteTable(table,joinTable);
		boolean _handleDataPerm = false;
		if(this.handleDataPerm && strategy != null) {
			if(joinTable) {
				_handleDataPerm = strategy.isHandleJoin(rewriteTable.getTableName());
			}else {
				_handleDataPerm  = strategy.isAllMatch() || strategy.hasTableStrategy(rewriteTable.getTableName());
			}
			//
			rewriteTable.setTableStrategy(strategy.getTableStrategy(rewriteTable.getTableName()));
		}
		
		Map<String, List<String>> columnMapping = null;
		//注解模式
		if (_handleDataPerm && !strategy.isAllMatch()) {
			DataPermissionItem tableStrategy = strategy.getTableStrategy(rewriteTable.getTableName());
			String[] columns = tableStrategy.columns();
			//
			columnMapping = new HashMap<>(columns.length + 2);
			String alias;
			List<String> tmpColumns;
			for (String column : columns) {
				if (column.contains(GlobalConstants.COLON)) {
					String[] parts = StringUtils.split(column, GlobalConstants.COLON);
					column = parts[0];
					alias = parts[1];
				} else {
					alias = handler.getDataPermColumnAlias(rewriteTable.getTableName(), column);
				}
				tmpColumns = columnMapping.get(alias);
				if(tmpColumns == null) {
					columnMapping.put(alias, tmpColumns = new ArrayList<>(3));
				}
				tmpColumns.add(column);
			}
			//合并通用列
			handler.mergeTableColumnMapping(columnMapping, rewriteTable.getTableName());
		}
		//全匹配模式
		if(_handleDataPerm && strategy.isAllMatch()) {
			columnMapping = handler.getTaleAllPermColumnMapping(rewriteTable.getTableName());
		}
		//未启用数据权限的情况合并通用列
		if(columnMapping == null) {
			columnMapping = new HashMap<>(3);
			handler.mergeTableColumnMapping(columnMapping, rewriteTable.getTableName());
		}
		rewriteTable.setRewriteColumnMapping(columnMapping);
		//分表配置
		Map<String, String> rewritedTableMapping = invocation.getTableNameMapping();
		final String originTableName = StringUtils.remove(table.getName(), RewriteTable.nameDelimiter);
		if(rewritedTableMapping != null && rewritedTableMapping.containsKey(originTableName)) {
			rewriteTable.setRewritedTableName(rewritedTableMapping.get(originTableName));
			table.setName(rewriteTable.getRewritedTableName());
		}
		
		rewriteTables.add(rewriteTable);
		return rewriteTable;
	}
	
	private void setTableOwnerColumns(SqlRewriteHandler handler,RewriteTable table) {
		boolean handleOwner = strategy.handleOwner(table.getTableName());
		if(handleOwner && !strategy.isAllMatch()) {
			handleOwner = MybatisConfigs.DATA_PERM_HANDLE_OWNER;
		}
		String[] ownerColumns = null; //当前用户关联的列
		if(handleOwner) {
			if(table.getTableStrategy() != null) {
				ownerColumns = table.getTableStrategy().ownerColumns();
			}
			if(!table.isUsingGlobalOrgPerm() 
					&& MybatisConfigs.DATA_PERM_HANDLE_OWNER 
					&& (ownerColumns == null || ownerColumns.length == 0)
					&& MetadataHelper.hasTableColumn(table.getTableName(),handler.getCreatedByColumnName())) {
				ownerColumns = new String[] {handler.getCreatedByColumnName()};
			}
		}
		table.setOwnerColumns(ownerColumns);
		if(traceLogging && ownerColumns != null) {
			logger.info("<trace_logging> set[{}]OwnerColumns:{}",table.getTableName(),ownerColumns);
		}
	}
	
	private boolean isUsingGroupOrgPerm(SqlRewriteHandler handler,RewriteTable table) {
		String createdByColumn = handler.getCreatedByColumnName();
		String deptPropName = handler.getDeptPropName();
		
		boolean withDeptColumn = withDeptmentItem(table, deptPropName);
		table.setWithDeptColumn(withDeptColumn);
		boolean withOwnerColumn = false;
		if(table.getTableStrategy() != null) {
			for (String column : table.getTableStrategy().ownerColumns()) {
				if(withOwnerColumn = (!column.equals(createdByColumn))) {
					break;
				}
			}
		}
        //
		for (RewriteTable thisTable : rewriteTables) {
			if(thisTable.getTableName().equals(table.getTableName())) {
				continue;
			}
			
			if(thisTable.isAppendConditonUsingOn()) {
				continue;
			}
			
			if(withOwnerColumn && withDeptmentItem(thisTable, deptPropName)) {
				return true;
			}
			
			if(withDeptColumn && thisTable.getTableStrategy() != null) {
				for (String column : thisTable.getTableStrategy().ownerColumns()) {
					if(!column.equals(createdByColumn)) {
						return true;
					}
				}
			}
			
		}
		return false;
	}
	
	private boolean withDeptmentItem(RewriteTable table,String deptPropName) {
		if(!table.getRewriteColumnMapping().containsKey(deptPropName))return false;
		String[] values = getDataPermValues().get(deptPropName);
		if(values == null)values = getDataPermValues().get(MybatisConfigs.ORG_DATA_PERM_NAME);
		if(values != null && (values.length != 1 || !DeptPermType.none.name().equals(values[0]))) {					
			return true;
		}
		return false;
	}
	
	public String toLogString() {
		StringBuilder builder = new StringBuilder();
		if(traceLogging)builder.append("<trace_logging> ");
		builder.append("RewriteSqlOnceContext[");
		builder.append(invocation.getMappedStatement().getId()).append("]");
		builder.append("\n - handleTenant:").append(handleTenant);
		builder.append("\n - handleDataPerm:").append(handleDataPerm);
		builder.append("\n - handleSoftDelete:").append(handleSoftDelete);
		builder.append("\n - handleOrderBy:").append(handleOrderBy);
		if(handleTenant)builder.append("\n - currentTenantId:").append(currentTenantId);
		if(rewriteTables != null) {
			builder.append("\n - rewriteTables:");
			if(traceLogging) {
				for (RewriteTable rewriteTable : rewriteTables) {
					builder.append("\n  - ").append(rewriteTable);
				}
			}else {
				builder.append(rewriteTables.stream().map(o -> o.getTableName()).collect(Collectors.toList()));
			}
		}
		return builder.toString();
	}
}
