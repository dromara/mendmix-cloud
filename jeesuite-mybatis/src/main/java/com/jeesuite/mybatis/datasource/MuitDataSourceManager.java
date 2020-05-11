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
	private static volatile MuitDataSourceManager instance = new MuitDataSourceManager();

	private MuitDataSourceManager() {
	}

	public static MuitDataSourceManager get() {
		return instance;
	}

	protected void registerDataSourceKey(String dsKey) {
		if (dsKey.contains("master")) {
			master = dsKey;
		} else {
			slaves.add(dsKey);
		}
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
}


