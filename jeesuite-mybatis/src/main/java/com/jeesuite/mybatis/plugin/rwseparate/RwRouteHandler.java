package com.jeesuite.mybatis.plugin.rwseparate;

import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.core.InterceptorType;
import com.jeesuite.mybatis.datasource.DataSourceContextHolder;
import com.jeesuite.mybatis.plugin.JeesuiteMybatisPluginContext;


/**
 * 读写分离自动路由处理
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 * @Copyright (c) 2015, jwww
 */
public class RwRouteHandler implements InterceptorHandler {

	protected static final Logger logger = LoggerFactory.getLogger(RwRouteHandler.class);

	@Override
	public Object onInterceptor(Invocation invocation) throws Throwable {
		//已指定强制使用当前设置的
		if(DataSourceContextHolder.get().isForceUseOne()){
			return null;
		}
		
		Object[] objects = invocation.getArgs();
		MappedStatement ms = (MappedStatement) objects[0];
		//读方法
		if(ms.getSqlCommandType().equals(SqlCommandType.SELECT)){
			//!selectKey 为自增id查询主键(SELECT LAST_INSERT_ID() )方法，使用主库
			if(!ms.getId().contains(SelectKeyGenerator.SELECT_KEY_SUFFIX)){				
				DataSourceContextHolder.get().useSlave(true);
				if(logger.isDebugEnabled()){
					logger.debug("设置方法[{}] use Slave Strategy..",ms.getId());
				}
			}
		}else{
			if(logger.isDebugEnabled()){
				logger.debug("设置方法[{}] use Master Strategy..",ms.getId());
			}
			DataSourceContextHolder.get().useSlave(false);
		}
		
		return null;
	}

	@Override
	public void onFinished(Invocation invocation,Object result) {
		
	}

	@Override
	public InterceptorType getInterceptorType() {
		return InterceptorType.before;
	}

	@Override
	public void start(JeesuiteMybatisPluginContext context) {}


	@Override
	public void close() {}
}
