/**
 * 
 */
package com.jeesuite.mybatis.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.mybatis.MybatisRuntimeContext;
import com.jeesuite.mybatis.plugin.JeesuiteMybatisInterceptor;

/**
 * 多数据源管理器
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年2月2日
 * @Copyright (c) 2015, jwww
 */
public class MuitDataSourceManager {

	private static final Logger logger = LoggerFactory.getLogger(MuitDataSourceManager.class);
	
	private final static AtomicLong counter = new AtomicLong(10);

	private static String master;
	private static final List<String> slaves = new ArrayList<>();
	private static volatile MuitDataSourceManager holder = new MuitDataSourceManager();

	private MuitDataSourceManager() {
	}

	public static MuitDataSourceManager get() {
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
	public MuitDataSourceManager useSlave(boolean useSlave) {
		DataSourceContextVals vals = MybatisRuntimeContext.getDataSourceContextVals();
		vals.userSlave = useSlave;
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
		
		DataSourceContextVals vals = MybatisRuntimeContext.getDataSourceContextVals();
		if(vals == null || StringUtils.isBlank(vals.dsKey)){
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
		DataSourceContextVals vals = MybatisRuntimeContext.getDataSourceContextVals();
		vals.forceMaster = true;
	}

	/**
	 * 判断是否强制使用一种方式
	 * 
	 * @return
	 */
	public boolean isForceUseMaster() {
		return MybatisRuntimeContext.getDataSourceContextVals().forceMaster;
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

	public static class DataSourceContextVals {
		public boolean userSlave; //
		public boolean forceMaster;
		public String dsKey;
	}
}


