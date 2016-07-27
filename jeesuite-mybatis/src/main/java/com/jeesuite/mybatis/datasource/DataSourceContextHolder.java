/**
 * 
 */
package com.jeesuite.mybatis.datasource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年2月2日
 * @Copyright (c) 2015, jwww
 */
public class DataSourceContextHolder {

	protected static final Logger logger = LoggerFactory.getLogger(DataSourceContextHolder.class);

	private static final Map<String, String> masters = new HashMap<>();
	private static final Map<String, List<String>> slaves = new HashMap<>();

	private final ThreadLocal<DataSourceContextVals> contextVals = new ThreadLocal<DataSourceContextVals>();

	private static DataSourceContextHolder holder;

	private DataSourceContextHolder() {
	}

	public static DataSourceContextHolder get() {
		if (holder == null)
			holder = new DataSourceContextHolder();
		return holder;
	}

	public void registerDataSourceKey(String dsKey) {
		String dbIndex = "0";
		if(dsKey.startsWith("group")){
			dbIndex = dsKey.split("\\_")[0].replace("group", "");
		}
		if (dsKey.contains("master")) {
			masters.put(dbIndex, dsKey);
		} else {
			List<String> sameDbSlaves = null;
			if (slaves.containsKey(dbIndex)) {
				sameDbSlaves = slaves.get(dbIndex);
			} else {
				sameDbSlaves = new ArrayList<>();
				slaves.put(dbIndex, sameDbSlaves);
			}
			sameDbSlaves.add(dsKey);
		}
	}

	/**
	 * 设置分库使用数据库序列（groupId）
	 * @param dbIndex
	 */
	public void setDbIndex(int dbIndex) {
		DataSourceContextVals vals = contextVals.get();
		if (vals == null)
			vals = new DataSourceContextVals();
		vals.dbIndex = dbIndex;
		contextVals.set(vals);
	}
	
	public void setShardFieldValue(Object shardFieldValue) {
		DataSourceContextVals vals = contextVals.get();
		if (vals == null)
			vals = new DataSourceContextVals();
		vals.shardFieldValue =shardFieldValue;
		contextVals.set(vals);
	}
	
	public Object getShardFieldValue() {
		DataSourceContextVals vals = contextVals.get();
		if (vals == null)return null;
		return vals.shardFieldValue;
	}

	/**
	 * 设置是否使用从库
	 * 
	 * @param slave
	 */
	public DataSourceContextHolder useSlave(boolean slave) {
		DataSourceContextVals vals = contextVals.get();
		if (vals == null)
			vals = new DataSourceContextVals();
		vals.userSlave = slave;
		contextVals.set(vals);
		return this;
	}

	/**
	 * 设置强制使用主库
	 * 
	 * @return
	 */
	public DataSourceContextHolder forceMaster() {
		DataSourceContextVals vals = contextVals.get();
		if (vals == null)
			vals = new DataSourceContextVals();
		vals.userOne = true;
		vals.userSlave = false;
		contextVals.set(vals);
		return this;
	}

	/**
	 * 设置强制使用从库
	 * 
	 * @return
	 */
	public DataSourceContextHolder forceSlave() {
		DataSourceContextVals vals = contextVals.get();
		if (vals == null)
			vals = new DataSourceContextVals();
		vals.userOne = true;
		vals.userSlave = true;
		contextVals.set(vals);
		return this;
	}

	/**
	 * 获取当前数据源名
	 * 
	 * @return
	 */
	public String getDataSourceKey() {
		DataSourceContextVals vals = contextVals.get();
		//TODO 
		if(vals == null)return masters.get(0);
		if (vals.userOne && vals.dsKey != null){
			if(logger.isDebugEnabled()){
				logger.debug("current force use dataSource key is [{}]!",vals.dsKey);
			}
			return vals.dsKey;
		}
		int dbGoupId = vals.dbIndex;
		String dsKey = null;
		if (vals.userSlave) {
			if (dbGoupId > 0 && slaves.size() <= dbGoupId + 1) {
				throw new RuntimeException("expect db group number is :" + dbGoupId + ",actaul:" + (dbGoupId + 1));
			}
			dsKey = getLruSlave(dbGoupId);
		} else {
			if (dbGoupId > 0 && masters.size() <= dbGoupId + 1) {
				throw new RuntimeException("expect db group number is :" + dbGoupId + ",actaul:" + (dbGoupId + 1));
			}
			dsKey = masters.get(String.valueOf(dbGoupId));
		}

		vals.dsKey = dsKey;
		
		if(logger.isDebugEnabled()){
			logger.debug("current use dataSource key is [{}]!",dsKey);
		}

		return dsKey;
	}

	/**
	 * 判断是否强制使用一种方式
	 * 
	 * @return
	 */
	public boolean isForceUseOne() {
		DataSourceContextVals vals = contextVals.get();
		if (vals == null)
			return false;
		return vals.userOne;
	}

	/**
	 * 获取最近最少使用的从数据库
	 * 
	 * @return
	 */
	private static String getLruSlave(Serializable dbIndex) {
		List<String> sameDbSlaves = slaves.get(dbIndex.toString());
		//  无从库则路由到主库
		if (sameDbSlaves == null || sameDbSlaves.isEmpty()) {
			String masterKey = masters.get(dbIndex.toString());
			if(logger.isDebugEnabled()){
				logger.debug("current no slave found ,default use [{}]!",masterKey);
			}
			return masterKey;
		}
		if (sameDbSlaves.size() == 1)
			return sameDbSlaves.get(0);
		// TODO
		String slaveKey = sameDbSlaves.get(RandomUtils.nextInt(0, sameDbSlaves.size()));
		return slaveKey;
	}

	public void clear() {
		contextVals.remove();
	}
	
	
	private class DataSourceContextVals {

		public int dbIndex;
		public boolean userSlave; //
		public boolean userOne;
		public String dsKey;
		public Object shardFieldValue;
	}
}


