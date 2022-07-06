package com.mendmix.mybatis.plugin.rewrite;

import java.util.HashMap;
import java.util.Map;

import com.mendmix.mybatis.plugin.rewrite.annotation.DataPermission;
import com.mendmix.mybatis.plugin.rewrite.annotation.DataPermissionItem;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年6月14日
 */
public class DataPermissionStrategy {

	private boolean allMatch;
	
	private boolean handleJoin; 
	
	private Map<String, DataPermissionItem> strategies;
	
	public DataPermissionStrategy(DataPermission annotation) {
		handleJoin = annotation.handleJoin();
		if(annotation.strategy().length == 0) {
			allMatch = true;
		}else {
			strategies = new HashMap<>(annotation.strategy().length);
			for (DataPermissionItem policy : annotation.strategy()) {
				strategies.put(policy.table(), policy);
			}
		}
	}
	
	public DataPermissionStrategy(boolean allMatch, Map<String, DataPermissionItem> strategies) {
		this.allMatch = allMatch;
		this.handleJoin = allMatch;
		this.strategies = strategies;
	}

	public boolean isAllMatch() {
		return allMatch;
	}

	public boolean isHandleJoin(String table) {
		if(!this.handleJoin)return false;
		if(!this.allMatch && !hasTableStrategy(table)) {
			return false;
		}
		return true;
	}

	public DataPermissionItem getTableStrategy(String table) {
		return strategies.get(table);
	}
	
	public boolean hasTableStrategy(String table) {
		return strategies.containsKey(table);
	}
	
	public boolean handleOwner(String table) {
		if(allMatch)return allMatch;
		return strategies.containsKey(table) ? getTableStrategy(table).handleOwner() : true;
	}
}
