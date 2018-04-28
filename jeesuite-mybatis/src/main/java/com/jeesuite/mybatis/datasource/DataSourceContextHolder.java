/**
 * 
 */
package com.jeesuite.mybatis.datasource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.mybatis.plugin.JeesuiteMybatisInterceptor;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年2月2日
 * @Copyright (c) 2015, jwww
 */
public class DataSourceContextHolder {

	private static final Logger logger = LoggerFactory.getLogger(DataSourceContextHolder.class);
	
	private final static AtomicLong counter = new AtomicLong(10);

	private static final Map<String, String> masters = new HashMap<>();
	private static final Map<String, List<String>> slaves = new HashMap<>();

	private final ThreadLocal<DataSourceContextVals> contextVals = new ThreadLocal<DataSourceContextVals>();

	private static volatile DataSourceContextHolder holder = new DataSourceContextHolder();

	private DataSourceContextHolder() {
	}

	public static DataSourceContextHolder get() {
		return holder;
	}

	protected void registerDataSourceKey(String dsKey) {
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
	
	/**
	 * 设置是否使用从库
	 * 
	 * @param useSlave
	 */
	public DataSourceContextHolder useSlave(boolean useSlave) {
		DataSourceContextVals vals = contextVals.get();
		if (vals == null)
			vals = new DataSourceContextVals();
		vals.userSlave = useSlave;
		contextVals.set(vals);
		return this;
	}
	
	/**
	 * 获取当前数据源名
	 * 
	 * @return
	 */
	protected String getDataSourceKey() {
		if(JeesuiteMybatisInterceptor.isRwRouteEnabled() == false && JeesuiteMybatisInterceptor.isDbShardEnabled() == false){
			return masters.get(0);
		}
		
		DataSourceContextVals vals = contextVals.get();
		if(vals == null){
			vals = new DataSourceContextVals();
			contextVals.set(vals);
			return masters.get(0);
		}
		
		int dbGoupId = vals.dbIndex;
		String dsKey = null;
		
        if (vals.forceMaster || !vals.userSlave){
			if (dbGoupId > 0 && masters.size() <= dbGoupId + 1) {
				throw new RuntimeException("expect db group number is :" + dbGoupId + ",actaul:" + (dbGoupId + 1));
			}
			dsKey = masters.get(String.valueOf(dbGoupId));
		}else{
			if (dbGoupId > 0 && slaves.size() <= dbGoupId + 1) {
				throw new RuntimeException("expect db group number is :" + dbGoupId + ",actaul:" + (dbGoupId + 1));
			}
			dsKey = selectSlave(dbGoupId);
		}
		
		vals.dsKey = dsKey;
		logger.debug("current route rule is:userSlave[{}]|forceMaster[{}], use dataSource key is [{}]!",vals.userSlave,vals.forceMaster,vals.dsKey);
		
		return dsKey;
	}
	
	/**
	 * 设置强制使用master库
	 */
	public void forceMaster(){
		DataSourceContextVals vals = contextVals.get();
		if(vals == null){
			vals = new DataSourceContextVals();
			vals.forceMaster = true;
			contextVals.set(vals);
		}
	}

	/**
	 * 判断是否强制使用一种方式
	 * 
	 * @return
	 */
	public boolean isForceUseMaster() {
		DataSourceContextVals vals = contextVals.get();
		if (vals == null)
			return false;
		return vals.forceMaster;
	}

	/**
	 * 轮循分配slave节点
	 * 
	 * @return
	 */
	private static String selectSlave(Serializable dbIndex) {
		List<String> sameDbSlaves = slaves.get(dbIndex.toString());
		//  无从库则路由到主库
		if (sameDbSlaves == null || sameDbSlaves.isEmpty()) {
			String masterKey = masters.get(dbIndex.toString());
			logger.debug("current no slave found ,default use [{}]!",masterKey);
			return masterKey;
		}
		if (sameDbSlaves.size() == 1)
			return sameDbSlaves.get(0);
		
		int selectIndex = (int) (counter.getAndIncrement() % sameDbSlaves.size());
		String slaveKey = sameDbSlaves.get(selectIndex);
		return slaveKey;
	}

	public void clear() {
		contextVals.remove();
	}
	
	
	private class DataSourceContextVals {

		public int dbIndex;
		public boolean userSlave; //
		public boolean forceMaster;
		public String dsKey;
	}
}


