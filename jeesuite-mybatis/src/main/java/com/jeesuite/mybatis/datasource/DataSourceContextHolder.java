/**
 * 
 */
package com.jeesuite.mybatis.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.mybatis.plugin.JeesuiteMybatisInterceptor;

/**
 * 数据源上下文
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年2月2日
 * @Copyright (c) 2015, jwww
 */
public class DataSourceContextHolder {

	private static final Logger logger = LoggerFactory.getLogger(DataSourceContextHolder.class);
	
	private final static AtomicLong counter = new AtomicLong(10);

	private static String master;
	private static final List<String> slaves = new ArrayList<>();

	private final ThreadLocal<DataSourceContextVals> contextVals = new ThreadLocal<DataSourceContextVals>();
	private static volatile DataSourceContextHolder holder = new DataSourceContextHolder();

	private DataSourceContextHolder() {
	}

	public static DataSourceContextHolder get() {
		return holder;
	}

	protected void registerDataSourceKey(String dsKey) {
		if (dsKey.contains("master")) {
			master = dsKey;
		} else {
			slaves.add(dsKey);
		}
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
		if(JeesuiteMybatisInterceptor.isRwRouteEnabled() == false){
			return master;
		}
		
		DataSourceContextVals vals = contextVals.get();
		if(vals == null){
			vals = new DataSourceContextVals();
			contextVals.set(vals);
			return master;
		}
		
		String dsKey = null;
		
        if (vals.forceMaster || !vals.userSlave){
			dsKey = master;
		}else{
			dsKey = selectSlave();
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
	private static String selectSlave() {
		//  无从库则路由到主库
		if (slaves.isEmpty()) {
			logger.debug("current no slave found ,default use [{}]!",master);
			return master;
		}
		if (slaves.size() == 1)
			return slaves.get(0);
		
		int selectIndex = (int) (counter.getAndIncrement() % slaves.size());
		String slaveKey = slaves.get(selectIndex);
		return slaveKey;
	}

	public void clear() {
		contextVals.remove();
	}
	
	
	private class DataSourceContextVals {
		public boolean userSlave; //
		public boolean forceMaster;
		public String dsKey;
	}
}


