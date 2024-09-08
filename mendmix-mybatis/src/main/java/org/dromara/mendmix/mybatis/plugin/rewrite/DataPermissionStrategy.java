package org.dromara.mendmix.mybatis.plugin.rewrite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.mybatis.MybatisConfigs;
import org.dromara.mendmix.mybatis.MybatisRuntimeContext;
import org.dromara.mendmix.mybatis.plugin.rewrite.annotation.DataPermission;
import org.dromara.mendmix.mybatis.plugin.rewrite.annotation.DataPermissionItem;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年6月14日
 */
public class DataPermissionStrategy {


	private boolean allMatch;
	
	private boolean handleJoin; 
	
	private boolean handleOwner;
	
	private boolean joinConditionWithOn;
	
	private boolean onlyHandleOwner;
	
	private Map<String, DataPermissionItem> strategies;
	
	public DataPermissionStrategy(DataPermission annotation) {
		handleJoin = annotation.handleJoin();
		handleOwner = annotation.handleOwner();
		joinConditionWithOn = annotation.joinConditionWithOn();
		if(annotation.strategy().length == 0) {
			allMatch = true;
		}else {
			strategies = new HashMap<>(annotation.strategy().length);
			for (DataPermissionItem policy : annotation.strategy()) {
				strategies.put(policy.table(), policy);
			}
			handleOwner = strategies.values().stream().anyMatch(a -> a.handleOwner());
		}
	}
	
	public DataPermissionStrategy(boolean allMatch, Map<String, DataPermissionItem> strategies) {
		this.allMatch = allMatch;
		this.handleJoin = allMatch;
		handleOwner = MybatisConfigs.DATA_PERM_HANDLE_OWNER;
		this.joinConditionWithOn = true;
		this.strategies = strategies;
		if(strategies != null) {
			this.handleOwner = strategies.values().stream().anyMatch(a -> a.handleOwner());
		}
	}

	public boolean isAllMatch() {
		return allMatch;
	}
	
	public void setHandleOwner(boolean handleOwner) {
		this.handleOwner = handleOwner;
	}

	public void setHandleJoin(boolean handleJoin) {
		this.handleJoin = handleJoin;
	}

	public boolean isJoinConditionWithOn() {
		return joinConditionWithOn;
	}
	
	public boolean isOnlyHandleOwner() {
		return onlyHandleOwner;
	}

	public void setOnlyHandleOwner(boolean onlyHandleOwner) {
		this.onlyHandleOwner = onlyHandleOwner;
	}

	public boolean isHandleJoin(String table) {
		if(!this.handleJoin)return false;
		if(!this.allMatch && !hasTableStrategy(table)) {
			return false;
		}
		return true;
	}

	public DataPermissionItem getTableStrategy(String table) {
		return strategies == null ? null : strategies.get(table);
	}
	
	public boolean hasTableStrategy(String table) {
		if(allMatch)return true;
		return strategies != null && strategies.containsKey(table);
	}
	
	public boolean handleOwner(String table) {
		if(allMatch || !handleOwner)return handleOwner;
		if(hasTableStrategy(table)) {
			return getTableStrategy(table).handleOwner();
		}else {
			return false;
		}
	}
	
	public List<List<ConditionPair>> getOrRelationColumns(String table){
		if(strategies == null || !strategies.containsKey(table))return null;
		String[] relations = strategies.get(table).orRelations();
		if(relations.length == 0)return null;
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
	
	public static void updateHandleOwner(boolean handleOwner) {
		DataPermissionStrategy strategy = MybatisRuntimeContext.getDataPermissionStrategy();
		if(strategy == null) {
			strategy = new DataPermissionStrategy(false, null);
			MybatisRuntimeContext.setDataPermissionStrategy(strategy);
		}
		strategy.setHandleOwner(handleOwner);
	}
}
