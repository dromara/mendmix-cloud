package com.jeesuite.mybatis.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.parser.MybatisMapperParser;
import com.jeesuite.mybatis.plugin.cache.CacheHandler;
import com.jeesuite.mybatis.plugin.pagination.PaginationHandler;
import com.jeesuite.mybatis.plugin.rwseparate.RwRouteHandler;
import com.jeesuite.mybatis.plugin.shard.DatabaseRouteHandler;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.spring.SpringInstanceProvider;

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
public class JeesuiteMybatisInterceptor implements Interceptor,InitializingBean,DisposableBean,ApplicationContextAware{

	private Properties properties;
	//CRUD框架驱动 default，mapper3
	private List<InterceptorHandler> interceptorHandlers = new ArrayList<>();
	
	private static boolean cacheEnabled,rwRouteEnabled,dbShardEnabled;
	
	//cache,rwRoute,dbShard
	public void setInterceptorHandlers(String interceptorHandlers) {
		String[] handlerNames = StringUtils.tokenizeToStringArray(interceptorHandlers, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
	    
		for (String name : handlerNames) {
			if("cache".equals(name)){
				this.interceptorHandlers.add(new CacheHandler());
				cacheEnabled = true;
			}else if("rwRoute".equals(name)){
				this.interceptorHandlers.add(new RwRouteHandler());
				rwRouteEnabled = true;
			}else if("dbShard".equals(name)){
				this.interceptorHandlers.add(new DatabaseRouteHandler());
				dbShardEnabled = true;
			}else if("page".equals(name)){
				this.interceptorHandlers.add(new PaginationHandler());
			}
		}
		//排序
		Collections.sort(this.interceptorHandlers, new Comparator<InterceptorHandler>() {
			@Override
			public int compare(InterceptorHandler o1, InterceptorHandler o2) {
				return Integer.compare(o1.interceptorOrder(), o2.interceptorOrder());
			}
		});
		
	}
	
	public void setMapperLocations(String mapperLocations){
		MybatisMapperParser.setMapperLocations(mapperLocations);
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		
		Object result = null;
		boolean proceed = false;
		for (InterceptorHandler handler : interceptorHandlers) {
			result = handler.onInterceptor(invocation);
			if(result != null)break;
		}
		
		if(result == null){
			result = invocation.proceed();
			proceed = true;
		}
		
		for (InterceptorHandler handler : interceptorHandlers) {
			handler.onFinished(invocation,proceed ? result : null);
		}
		
		return result;
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
		this.properties = properties;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		Iterator<InterceptorHandler> it = interceptorHandlers.iterator();
		while(it.hasNext()){
			InterceptorHandler handler = it.next();
			handler.start(this);
		}
	}

	@Override
	public void destroy() throws Exception {
		for (InterceptorHandler handler : interceptorHandlers) {
			handler.close();
		}
	}

	public String getProperty(String key){
		return properties == null ? null : properties.getProperty(key);
	}
	
	public String getProperty(String key,String defaultVal){
		String property = getProperty(key);
		return property == null ? defaultVal : property;
	}
	
	public static boolean isCacheEnabled() {
		return cacheEnabled;
	}

	public static boolean isRwRouteEnabled() {
		return rwRouteEnabled;
	}

	public static boolean isDbShardEnabled() {
		return dbShardEnabled;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		InstanceFactory.setInstanceProvider(new SpringInstanceProvider(applicationContext));
	}

}
