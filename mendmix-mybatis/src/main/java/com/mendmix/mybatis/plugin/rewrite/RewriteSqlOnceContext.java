package com.mendmix.mybatis.plugin.rewrite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.GlobalConstants;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.model.AuthUser;
import com.mendmix.common.model.DataPermItem;
import com.mendmix.common.model.PageParams;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.mybatis.MybatisConfigs;
import com.mendmix.mybatis.MybatisRuntimeContext;
import com.mendmix.mybatis.metadata.MetadataHelper;
import com.mendmix.mybatis.plugin.InvocationVals;
import com.mendmix.mybatis.plugin.rewrite.annotation.TablePermissionStrategy;

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
 * @date 2023年4月25日
 */
public class RewriteSqlOnceContext {
	
	private final static Logger logger = LoggerFactory.getLogger("com.mendmix.mybatis");
	
	private static UserPermissionProvider userPermissionProvider = MybatisRuntimeContext.getUserPermissionProvider();
	
	InvocationVals invocation;
	SqlRewriteStrategy strategy;
	private Map<String, String[]> dataPermValues;
	boolean handleDataPerm;
	boolean handleTenant;
	boolean handleSoftDelete;
	boolean handleOrderBy;
	
	String currentGroupKey;
	
	List<RewriteTable> rewriteTables;
	
	AuthUser currentUser;
	String currentTenantId;
	
	boolean loadedCurrentGroupPermData;
	boolean mainTableHandledTenant; //主表已处理租户
	
	boolean traceLogging = CurrentRuntimeContext.isDebugMode();

	public RewriteSqlOnceContext(InvocationVals invocation,boolean isFieldTenantMode ,boolean dynaDataPermEnaled) {
		this.invocation = invocation;
		currentUser = CurrentRuntimeContext.getCurrentUser();
		strategy = MybatisRuntimeContext.getSqlRewriteStrategy();
		this.handleDataPerm = dynaDataPermEnaled 
				&& (currentUser != null && !currentUser.isAdmin())
				&& strategy.handleDataPerm();
		this.handleTenant = isFieldTenantMode 
				&& !strategy.isIgnoreTenant();
		PageParams pageParam = invocation.getPageParam();
		this.handleOrderBy = pageParam != null && pageParam.getOrderBys() != null && !pageParam.getOrderBys().isEmpty();
		if(isFieldTenantMode) {
			currentTenantId = CurrentRuntimeContext.getTenantId();
		}
	}
	
	
	public Map<String, String[]> getDataPermValues() {
		if(dataPermValues == null && handleDataPerm) {
			dataPermValues = MybatisRuntimeContext.getDataPermissionValues();
		}
		if(dataPermValues == null) {
			dataPermValues = new HashMap<>(0);
			if(handleDataPerm)MybatisRuntimeContext.setDataPermissionValues(dataPermValues, false);
		}
		return dataPermValues;
	}

    public boolean withTableShardingRule() {
    	return strategy.getRewritedTableMapping() != null && !strategy.getRewritedTableMapping().isEmpty();
    }

	public boolean withConditionReriteRule() {
		return handleDataPerm 
			   || handleTenant 
			   || handleSoftDelete 
			   || handleOrderBy;
	}
	
	public boolean withOwnerPermission() {
		if(getDataPermValues() == null || !getDataPermValues().containsKey(MybatisConfigs.DATA_PERM_SPEC_KEY)) {
			return false;
		}
		String[] values = getDataPermValues().get(MybatisConfigs.DATA_PERM_SPEC_KEY);
		for (String val : values) {
			if(SpecialPermType.owner.name().equals(val))return true;
		}
		return false;
	} 
	
	public String[] getPermValues(String field) {
		return getDataPermValues().get(field);
	}
	
	public void loadDataPermValues(SqlRewriteHandler handler) {
		if(!handleDataPerm || loadedCurrentGroupPermData)return;
		final boolean withGroup = StringUtils.isNotBlank(currentGroupKey);
		try {
			if(withGroup) {
				if(traceLogging)logger.info("<TRACE_LOGGING> reloadGroupDataPermValues:{}",currentGroupKey);
				mainTableHandledTenant = false;
				ThreadLocalContext.set(SpecialPermissionHelper.CONTEXT_CURRENT_PERM_GROUP, currentGroupKey);	
				//当前部门
				String positionId = currentGroupKey.substring(currentGroupKey.indexOf(GlobalConstants.COLON) + 1);
				ThreadLocalContext.set(SpecialPermissionHelper.CONTEXT_CURRENT_POSTION_ID, positionId);	
				dataPermValues = null;
				MybatisRuntimeContext.setDataPermissionValues(null, false);
			}
			dataPermValues = getDataPermValues();
			
			if(rewriteTables != null) {
				//加载部门数据
				if(handler.getDeptPropName() != null 
						&& dataPermValues.containsKey(MybatisConfigs.DATA_PERM_SPEC_KEY)) {
					if(rewriteTables.stream().anyMatch(
			    			o -> o.getRewriteColumnMapping().containsKey(handler.getDeptPropName()))
			    	) {
			    		SpecialPermissionHelper.prepareOrganizationPermission(invocation, dataPermValues, handler.getDeptPropName());
			    		if(withGroup) {
			    			if(traceLogging)logger.info("<TRACE_LOGGING> reloadPrepareOrganizationPermission for permGroup:{}",currentGroupKey);
			    			//由于第一次初始化的原因，这个不能放finally
				    		ThreadLocalContext.remove(SpecialPermissionHelper.CONTEXT_CURRENT_POSTION_ID);
			    		}
			    	}
				}
				//
				if(currentUser != null) {
					for (RewriteTable rewriteTable : rewriteTables) {
						rewriteTable.setOwnerColumns(null);
						setTableOwnerColumns(handler, rewriteTable);
					}
				}
			}
			if(traceLogging) {
				logger.info("<TRACE_LOGGING> loadDataPermValues\n -group:{}\n -values:{}",currentGroupKey,JsonUtils.toJson(getDataPermValues()));
			}
			loadedCurrentGroupPermData = true;
		} finally {
			if(withGroup)ThreadLocalContext.remove(SpecialPermissionHelper.CONTEXT_CURRENT_PERM_GROUP);
		}
	}

	public List<String> getPermGroupKeys(){
		if(!handleDataPerm) {
			return null;
		}
		List<DataPermItem> permssions = userPermissionProvider.findCurrentAllPermissions();
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
				).allMatch(o -> (o.isAllMatch() || (o.getValues() != null && o.getValues().contains(SpecialPermType._allValues.name()))) );
			   if(withAllPerm) {
				   if(traceLogging) {
						logger.info("<TRACE_LOGGING> permissionGroup:{} with AllPermission!!!",groupKey);
					}
				   return Arrays.asList(SpecialPermType._allValues.name());
			   }
			}
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
					boolean appendConditonUsingOn = !join.isInner() && join.getOnExpressions().size() > 0;
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

		if(traceLogging) {
			logger.info(this.toLogString());
		}else if(logger.isDebugEnabled()) {
			logger.debug(this.toLogString());
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
			TablePermissionStrategy tableStrategy = strategy.getTableStrategy(rewriteTable.getTableName());
			String[] columns = tableStrategy.columns();
			//
			columnMapping = new HashMap<>(columns.length + 2);
			String alias;
			for (String column : columns) {
				if (column.contains(GlobalConstants.COLON)) {
					String[] parts = StringUtils.split(column, GlobalConstants.COLON);
					column = parts[0];
					alias = parts[1];
				} else {
					alias = handler.getDataPermColumnAlias(rewriteTable.getTableName(), column);
				}
				columnMapping.put(alias, Arrays.asList(column));
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
		Map<String, String> rewritedTableMapping = strategy.getRewritedTableMapping();
		if(rewritedTableMapping != null && rewritedTableMapping.containsKey(table.getName())) {
			rewriteTable.setRewritedTableName(rewritedTableMapping.get(table.getName()));
			table.setName(rewriteTable.getRewritedTableName());
		}
		
		rewriteTables.add(rewriteTable);
		return rewriteTable;
	}
	
	private void setTableOwnerColumns(SqlRewriteHandler handler,RewriteTable table) {
		boolean handleOwner = strategy.handleOwner(table.getTableName());
		if(handleOwner && !strategy.isAllMatch()) {
			handleOwner = MybatisConfigs.DATA_PERM_DEFAULT_HANDLE_OWNER || withOwnerPermission();
		}
		String[] ownerColumns = null; //当前用户关联的列
		if(handleOwner) {
			if(table.getTableStrategy() != null) {
				ownerColumns = table.getTableStrategy().ownerColumns();
			}
			if(MybatisConfigs.DATA_PERM_DEFAULT_HANDLE_OWNER 
					&& (ownerColumns == null || ownerColumns.length == 0)
					&& MetadataHelper.hasTableColumn(table.getTableName(),handler.getCreatedByColumnName())) {
				ownerColumns = new String[] {handler.getCreatedByColumnName()};
			}
		}
		table.setOwnerColumns(ownerColumns);
	}
	
	public String toLogString() {
		StringBuilder builder = new StringBuilder();
		if(traceLogging)builder.append("<TRACE_LOGGING> ");
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
