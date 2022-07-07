package com.mendmix.mybatis.plugin.rewrite;

import java.util.HashMap;
import java.util.Map;

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
	
	private boolean ignoreTenant;
	
	private boolean ignoreSoftDelete;
	
	private boolean ignoreColumnPerm;
	
	private boolean allMatch = true;
	
	private boolean handleJoin = true; 
	
	private boolean handleOrderBy = true; 
	
	private Map<String, TablePermissionStrategy> tableStrategies;
	
	public void setDataPermission(DataPermission annotation) {
		handleJoin = annotation.handleJoin();
		if(annotation.strategy().length > 0) {
			allMatch = false;
			tableStrategies = new HashMap<>(annotation.strategy().length);
			for (TablePermissionStrategy policy : annotation.strategy()) {
				tableStrategies.put(policy.table(), policy);
			}
		}
	}
	
	public SqlRewriteStrategy(boolean allMatch, Map<String, TablePermissionStrategy> strategies) {
		this.allMatch = allMatch;
		this.handleJoin = allMatch;
		this.tableStrategies = strategies;
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

	public boolean isIgnoreTenant() {
		return ignoreTenant;
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

	public boolean isHandleJoin(String table) {
		if(!this.handleJoin)return false;
		if(!this.allMatch && !hasTableStrategy(table)) {
			return false;
		}
		return true;
	}

	public TablePermissionStrategy getTableStrategy(String table) {
		return tableStrategies.get(table);
	}
	
	public boolean hasTableStrategy(String table) {
		return tableStrategies.containsKey(table);
	}
	
	public boolean handleOwner(String table) {
		if(allMatch)return allMatch;
		return tableStrategies.containsKey(table) ? getTableStrategy(table).handleOwner() : true;
	}
}
