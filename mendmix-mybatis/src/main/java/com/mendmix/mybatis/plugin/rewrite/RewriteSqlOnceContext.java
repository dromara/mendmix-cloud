package com.mendmix.mybatis.plugin.rewrite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.GlobalConstants;
import com.mendmix.common.model.AuthUser;
import com.mendmix.common.model.PageParams;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.mybatis.MybatisConfigs;
import com.mendmix.mybatis.MybatisRuntimeContext;
import com.mendmix.mybatis.metadata.MetadataHelper;
import com.mendmix.mybatis.plugin.InvocationVals;
import com.mendmix.mybatis.plugin.rewrite.annotation.TablePermissionStrategy;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;

/**
 * 
 * <br>
 * @author jiangwei
 * @version 1.0.0
 * @date 2023年4月22日
 */
public class RewriteSqlOnceContext {
	
	private final static Logger logger = LoggerFactory.getLogger("com.zvosframework");
	
	InvocationVals invocation;
	SqlRewriteStrategy strategy;
	private Map<String, String[]> dataPermValues;
	boolean handleDataPerm;
	boolean handleTenant;
	boolean handleSoftDelete;
	boolean handleOrderBy;
	
	List<RewriteTable> rewriteTables;
	
	AuthUser currentUser;
	String currentTenantId;
	
	boolean mainTableHandledTenant; //主表已处理租户
	
	boolean traceLogging = CurrentRuntimeContext.isDebugMode();

	public RewriteSqlOnceContext(InvocationVals invocation,boolean isFieldTenantMode ,boolean dynaDataPermEnaled) {
		this.strategy = MybatisRuntimeContext.getSqlRewriteStrategy();
		this.currentUser = CurrentRuntimeContext.getCurrentUser();
		this.invocation = invocation;
		this.handleDataPerm = dynaDataPermEnaled 
				&& (currentUser != null && !currentUser.isAdmin())
				&& !this.strategy.isIgnoreDataPerm();
		this.handleTenant = isFieldTenantMode 
				&& !this.strategy.isIgnoreTenant();
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



	public boolean withReriteRule() {
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
	
	public boolean isHandleGroupOrgPermission() {
		if(rewriteTables.size() <= 1)return false;
		boolean has = rewriteTables.stream().anyMatch(o -> o.isUsingGroupOrgPerm());
		if(!has)return has;
		String[] values = getDataPermValues().get(MybatisConfigs.DATA_PERM_SPEC_KEY);
		if(values == null)return false;
		for (String val : values) {
			if(has = !SpecialPermType.owner.name().equals(val)) {
				return has;
			}
		}
		return false;
	}
	
	
	public void parseRewriteTable(SqlRewriteHandler handler,PlainSelect select) {
		if(rewriteTables == null) {
			rewriteTables = new ArrayList<>();
		}else {
			rewriteTables.clear();
		}
		Table table = (Table) select.getFromItem();
		RewriteTable rewriteTable = buildRewriteTable(handler,table, false);
		if(rewriteTable.getRewritedTableName() != null) {
			select.setFromItem(rewriteTable.getTable());
		}
		rewriteTable.setAppendConditonTo(select);
		
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
					boolean appendConditonUsingOn = !join.isInner();
					rewriteTable.setAppendConditonUsingOn(appendConditonUsingOn);
					if(appendConditonUsingOn) {
						rewriteTable.setAppendConditonTo(join);
					}else {
						rewriteTable.setAppendConditonTo(select);
					}
					
				}
			}
		}
		
        if(handleDataPerm) {
        	//预处理部门权限数据
    		String deptPropName = handler.getDeptPropName();
    		boolean prepareDeptPermData = getDataPermValues().containsKey(deptPropName);
    		if(!prepareDeptPermData && rewriteTables.stream().anyMatch(
    			o -> o.getRewriteColumnMapping().containsKey(deptPropName))
    		  ) {
    			handler.prepareSpecialPermValues(invocation, dataPermValues);
    			prepareDeptPermData = true;
    		}
    		//
    		for (RewriteTable _table : rewriteTables) {
    			//onwer 列
    			setTableOwnerColumns(handler, _table);
    			//使用启用多表组织权限统一分组处理
    			if(prepareDeptPermData && rewriteTables.size() > 1) {				
    				_table.setUsingGroupOrgPerm(isUsingGroupOrgPerm(handler, _table));
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
		
		Map<String, String> columnMapping = null;
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
				columnMapping.put(alias, column);
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
			if(!table.isUsingGroupOrgPerm() 
					&& MybatisConfigs.DATA_PERM_DEFAULT_HANDLE_OWNER 
					&& (ownerColumns == null || ownerColumns.length == 0)
					&& MetadataHelper.hasTableColumn(table.getTableName(),handler.getCreatedByColumnName())) {
				ownerColumns = new String[] {handler.getCreatedByColumnName()};
			}
		}
		table.setOwnerColumns(ownerColumns);
	}
	
	private boolean isUsingGroupOrgPerm(SqlRewriteHandler handler,RewriteTable table) {
		String createdByColumn = handler.getCreatedByColumnName();
		String deptPropName = handler.getDeptPropName();
		
		boolean withDeptColumn = table.getRewriteColumnMapping().containsKey(deptPropName);
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
			
			if(withOwnerColumn && thisTable.getRewriteColumnMapping().containsKey(deptPropName)) {
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
	
	public String toLogString() {
		StringBuilder builder = new StringBuilder("RewriteSqlOnceContext[");
		builder.append(invocation.getMappedStatement().getId()).append("]");
		builder.append("\n - handleTenant:").append(handleTenant);
		builder.append("\n - handleDataPerm:").append(handleDataPerm);
		builder.append("\n - handleSoftDelete:").append(handleSoftDelete);
		builder.append("\n - handleOrderBy:").append(handleOrderBy);
		if(handleTenant)builder.append("\n - currentTenantId:").append(currentTenantId);
		if(handleDataPerm) {
			if(traceLogging) {
				builder.append("\n - dataPermValues:").append(JsonUtils.toJson(getDataPermValues()));
			}else {
				builder.append("\n - dataPermValues:").append(getDataPermValues().keySet());
			}
		}
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
