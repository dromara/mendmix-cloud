package com.mendmix.mybatis.plugin.rewrite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.common.GlobalConstants;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.constants.ContextKeys;
import com.mendmix.mybatis.MybatisConfigs;
import com.mendmix.mybatis.plugin.rewrite.annotation.DataPermission;
import com.mendmix.mybatis.plugin.rewrite.annotation.TablePermissionStrategy;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年6月14日
 */
public class SqlRewriteStrategy {

	private boolean ignoreAny;
	
	private boolean ignoreDataPerm;
	
	private boolean ignoreTenant;
	
	private boolean ignoreSoftDelete;
	
	private boolean ignoreColumnPerm;
	
	private boolean allMatch = true;
	
	private boolean handleOrderBy = true; 
	
	private boolean handleJoin = true; 
	
	private boolean handleOwner = false;
	
	private Map<String, String> rewritedTableMapping;
	
	private Map<String, TablePermissionStrategy> tableStrategies;
	
	public void setDataPermission(DataPermission annotation) {
		ignoreColumnPerm = annotation.ignore();
		handleJoin = annotation.handleJoin();
		handleOwner = annotation.handleOwner();
		if(annotation.strategy().length > 0) {
			allMatch = false;
			tableStrategies = new HashMap<>(annotation.strategy().length);
			for (TablePermissionStrategy policy : annotation.strategy()) {
				tableStrategies.put(policy.table(), policy);
			}
			handleOwner = tableStrategies.values().stream().anyMatch(o -> o.handleOwner());
		}
	}
	
	public SqlRewriteStrategy(boolean allMatch, Map<String, TablePermissionStrategy> strategies) {
		this.allMatch = allMatch;
		this.handleOwner = MybatisConfigs.DATA_PERM_DEFAULT_HANDLE_OWNER;
		this.tableStrategies = strategies;
		if(strategies != null && !strategies.isEmpty()) {
			handleOwner = tableStrategies.values().stream().anyMatch(o -> o.handleOwner());
		}
	}
	

	public boolean isIgnoreAny() {
		return ignoreAny;
	}

	public void setIgnoreAny(boolean ignoreAny) {
		this.ignoreAny = ignoreAny;
	}

	public boolean isAllMatch() {
		return allMatch;
	}
	
	public boolean isIgnoreDataPerm() {
		return ignoreDataPerm;
	}

	public void setIgnoreDataPerm(boolean ignoreDataPerm) {
		this.ignoreDataPerm = ignoreDataPerm;
	}

	public boolean isIgnoreTenant() {
		return ignoreTenant || ThreadLocalContext.exists(ContextKeys.IGNORE_TENENT_ID);
	}

	public void setIgnoreTenant(boolean ignoreTenant) {
		this.ignoreTenant = ignoreTenant;
	}

	public boolean isIgnoreSoftDelete() {
		return ignoreSoftDelete;
	}

	public void setIgnoreSoftDelete(boolean ignoreSoftDelete) {
		this.ignoreSoftDelete = ignoreSoftDelete;
	}

	public boolean isIgnoreColumnPerm() {
		return ignoreColumnPerm;
	}

	public void setIgnoreColumnPerm(boolean ignoreColumnPerm) {
		this.ignoreColumnPerm = ignoreColumnPerm;
	}
	
	public boolean isHandleOrderBy() {
		return handleOrderBy;
	}

	public void setHandleOrderBy(boolean handleOrderBy) {
		this.handleOrderBy = handleOrderBy;
	}

	public void setHandleOwner(boolean handleOwner) {
		this.handleOwner = handleOwner;
	}

	public TablePermissionStrategy getTableStrategy(String table) {
		if(tableStrategies == null)return null;
		return tableStrategies.get(table);
	}
	
	public boolean hasTableStrategy(String table) {
		if(allMatch)return true;
		if(tableStrategies == null)return false;
		return tableStrategies.containsKey(table);
	}
	
	public boolean handleOwner(String table) {
		if(allMatch)return handleOwner;
		return hasTableStrategy(table) ? getTableStrategy(table).handleOwner() : true;
	}

	public boolean isHandleJoin() {
		return handleJoin;
	}
	
	public Map<String, String> getRewritedTableMapping() {
		return rewritedTableMapping;
	}

	public void setRewritedTableMapping(Map<String, String> rewritedTables) {
		this.rewritedTableMapping = rewritedTables;
	}
	
	public boolean isHandleJoin(String table) {
		if(!this.handleJoin)return false;
		if(!this.allMatch && !hasTableStrategy(table)) {
			return false;
		}
		return true;
	}
	
	public List<List<ConditionPair>> getOrRelationColumns(String table){
		if(tableStrategies == null || !tableStrategies.containsKey(table))return null;
		String[] relations = tableStrategies.get(table).orRelations();
		List<List<ConditionPair>> result = new ArrayList<>(relations.length);
		String[] columns;
		List<ConditionPair> conditions;
        for (String rel : relations) {
        	columns = StringUtils.split(rel, GlobalConstants.COMMA);
        	conditions = new ArrayList<>(columns.length);
            for (String column : columns) {
            	conditions.add(new ConditionPair(column, null));
			}
        	result.add(conditions);
		}
        return result;
	}
	
}
