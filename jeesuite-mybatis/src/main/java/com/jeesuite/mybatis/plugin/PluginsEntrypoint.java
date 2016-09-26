package com.jeesuite.mybatis.plugin;

import java.util.List;
import java.util.Properties;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.core.InterceptorType;
import com.jeesuite.mybatis.plugin.cache.CacheHandler;

/**
 * mybatis 插件入口
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 * @Copyright (c) 2015, jwww
 */
@Intercepts({  
    @Signature(type = Executor.class, method = "update", args = {  
            MappedStatement.class, Object.class }),  
    @Signature(type = Executor.class, method = "query", args = {  
            MappedStatement.class, Object.class, RowBounds.class,  
            ResultHandler.class }) })  
public class PluginsEntrypoint implements Interceptor{

	private List<InterceptorHandler> interceptorHandlers;
	
	public void setInterceptorHandlers(List<InterceptorHandler> interceptorHandlers) {
		this.interceptorHandlers = interceptorHandlers;
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		
		boolean proceed = false;
		Object result = null;
		try {
			for (InterceptorHandler handler : interceptorHandlers) {
				Object object = handler.onInterceptor(invocation);
				if(handler.getInterceptorType().equals(InterceptorType.around)){
					result = object;
					//查询缓存命中，则不执行分库和读写分离的处理器
					if(result != null && handler instanceof CacheHandler){
						break;
					}
				}
			}
			if(result == null){
				result = invocation.proceed();
				proceed = true;
			}
			return result;
		} finally {
			for (InterceptorHandler handler : interceptorHandlers) {
				handler.onFinished(invocation,proceed ? result : null);
			}
		}
	}

	@Override
	public Object plugin(Object target) {
		if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }


	}

	@Override
	public void setProperties(Properties properties) {
		
	}

}
