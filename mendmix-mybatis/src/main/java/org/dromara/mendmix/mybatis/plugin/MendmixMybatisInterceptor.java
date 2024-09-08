/*
 * Copyright 2016-2020 dromara.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dromara.mendmix.mybatis.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.mybatis.MybatisConfigs;
import org.dromara.mendmix.mybatis.MybatisRuntimeContext;
import org.dromara.mendmix.mybatis.datasource.DataSoureConfigHolder;
import org.dromara.mendmix.mybatis.plugin.autofield.AutoFieldFillHandler;
import org.dromara.mendmix.mybatis.plugin.cache.CacheHandler;
import org.dromara.mendmix.mybatis.plugin.deletebackup.DeleteBackupHandler;
import org.dromara.mendmix.mybatis.plugin.operProtect.SensitiveOperProtectHandler;
import org.dromara.mendmix.mybatis.plugin.pagination.PaginationHandler;
import org.dromara.mendmix.mybatis.plugin.rewrite.SqlRewriteHandler;
import org.dromara.mendmix.mybatis.plugin.rwseparate.RwRouteHandler;
import org.dromara.mendmix.mybatis.plugin.sensitive.SensitiveCryptHandler;
import org.dromara.mendmix.mybatis.plugin.shard.TableShardingHandler;
import org.dromara.mendmix.mybatis.plugin.timezone.TimeZoneConvertHandler;
import org.dromara.mendmix.spring.InstanceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

/**
 * mybatis 插件入口
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 * @Copyright (c) 2015, jwww
 */
@Intercepts({ 
	//@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
    @Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class }),  
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class }) })  
public class MendmixMybatisInterceptor implements Interceptor,DisposableBean{

	protected static final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.mybatis");
	
	private String groupName;
	private List<PluginInterceptorHandler> interceptorHandlers = new ArrayList<>();
	
	private SqlRewriteHandler sqlRewriteHandler;
	
	private static boolean cacheEnabled,rwRouteEnabled;
	
	private DataSource dataSource;

	public MendmixMybatisInterceptor(String groupName, String[] hanlderNames) {
		this(groupName);
	}
	
	public MendmixMybatisInterceptor(String groupName) {
		this.groupName = groupName;
		//
		if(ResourceUtils.getBoolean("mendmix-cloud.mybatis.sqlRewrite.enabled",true)) {
			this.interceptorHandlers.add(sqlRewriteHandler = new SqlRewriteHandler());
		}
		if(ResourceUtils.getBoolean("mendmix-cloud.mybatis.autoField.enabled",true)) {
			this.interceptorHandlers.add(new AutoFieldFillHandler());
		}
		if(ResourceUtils.getBoolean("mendmix-cloud.mybatis.pagination.enabled",true)) {
			this.interceptorHandlers.add(new PaginationHandler());
		}
		if(cacheEnabled = MybatisConfigs.isCacheEnabled(groupName)) {
			this.interceptorHandlers.add(new CacheHandler());
		}
        if(rwRouteEnabled = DataSoureConfigHolder.containsSlaveConfig()) {
        	this.interceptorHandlers.add(new RwRouteHandler());
		}
		if(ResourceUtils.getBoolean("mendmix-cloud.mybatis.timeToUTC.enabled",false)) {
			this.interceptorHandlers.add(new TimeZoneConvertHandler());
		}
		if(ResourceUtils.getBoolean("mendmix-cloud.mybatis.sensitiveCrypt.enabled",false)) {
			this.interceptorHandlers.add(new SensitiveCryptHandler());
		}
		
		if(ResourceUtils.getBoolean("mendmix-cloud.mybatis.tableshard.enabled",false)) {
			this.interceptorHandlers.add(new TableShardingHandler());
		}
		
		if(ResourceUtils.getBoolean("mendmix-cloud.mybatis.tableshard.enabled",false)) {
			this.interceptorHandlers.add(new TableShardingHandler());
		}
		
		if(ResourceUtils.getBoolean("mendmix-cloud.mybatis.operProtect.enabled",false)) {
			this.interceptorHandlers.add(new SensitiveOperProtectHandler());
		}
		
		if(ResourceUtils.getBoolean("mendmix-cloud.mybatis.deletebackup.enabled",true)) {
			this.interceptorHandlers.add(new DeleteBackupHandler());
		}
		//
		this.initCustomInterceptorHandlers();
		//排序 分页和数据权限会重写sql，所以不走缓存，所以放在缓存之前
		Collections.sort(this.interceptorHandlers, new Comparator<PluginInterceptorHandler>() {
			@Override
			public int compare(PluginInterceptorHandler o1, PluginInterceptorHandler o2) {
				return Integer.compare(o1.interceptorOrder(), o2.interceptorOrder());
			}
		});
	}

	private void initCustomInterceptorHandlers() {
		//自定义的拦截器
		List<String> handlerNames = MybatisConfigs.getCustomHandlerNames(groupName);
		for (String name : handlerNames) {
			try {
				Class<?> clazz = Class.forName(name);
				PluginInterceptorHandler handler = (PluginInterceptorHandler) clazz.newInstance();
				this.interceptorHandlers.add(handler);
				logger.info("<startup-logging>  registered cumstom InterceptorHandler:{}",name);
			} catch (Exception e) {
				logger.error("registered cumstom InterceptorHandler error",e);
			}
		}
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		if(MybatisRuntimeContext.isIgnoreAny()) {
			return invocation.proceed();
		}
		String groupName = this.groupName;
		OnceContextVal invocationVal = new OnceContextVal(groupName,invocation);
		Object result = null;
		boolean cacheHited = false;
		for (PluginInterceptorHandler handler : interceptorHandlers) {
			result = handler.onInterceptor(invocationVal);
			if(result != null) {
				cacheHited = handler.getClass() == CacheHandler.class;
				break;
			}
		}
		
		if(result == null){
			result = invocation.proceed();
		}
        //
		for (PluginInterceptorHandler handler : interceptorHandlers) {
			if(cacheHited && handler.getClass() == CacheHandler.class)continue;
			try {					
				handler.onFinished(invocationVal,result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//按主键查询权限过滤
		if(result != null 
				&& invocationVal.isSelectByPrimaryKey() 
				&& !invocationVal.isSqlRewrited()
				&& sqlRewriteHandler != null) {
			try {
				@SuppressWarnings("unchecked")
				List<Object> asList = (List<Object>) result;
				if(!asList.isEmpty() && !sqlRewriteHandler.matchRewriteStrategy(invocationVal, asList.get(0))) {
					if(logger.isDebugEnabled())logger.debug("<soft_dataFilter_trace> {} 命中记录：{},不匹配当前数据权限",invocationVal.getMappedStatement().getId(),asList.size());
					asList.clear();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		MybatisRuntimeContext.unsetOnceContext();
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
	public void setProperties(Properties properties) {}


	public void afterRegister()  {
		Map<String, DataSource> dataSources = InstanceFactory.getBeansOfType(DataSource.class);
		if(dataSources.size() == 1) {
			dataSource = new ArrayList<>(dataSources.values()).get(0);
		}else {
			for (String beanName : dataSources.keySet()) {
				if(beanName.startsWith(groupName)) {
					dataSource = dataSources.get(beanName);
					break;
				}
			}
		}
		//
		Iterator<PluginInterceptorHandler> it = interceptorHandlers.iterator();
		while(it.hasNext()){
			PluginInterceptorHandler handler = it.next();
			handler.start(this);
		}
	}

	@Override
	public void destroy() throws Exception {
		for (PluginInterceptorHandler handler : interceptorHandlers) {
			handler.close();
		}
	}
	
	public static boolean isCacheEnabled() {
		return cacheEnabled;
	}

	public static boolean isRwRouteEnabled() {
		return rwRouteEnabled;
	}
	
	public String getGroupName() {
		return groupName;
	}

	public DataSource getDataSource() {
		return dataSource;
	}
	
	public List<PluginInterceptorHandler> getInterceptorHandlers() {
		return interceptorHandlers;
	}

	@SuppressWarnings("unchecked")
	public <T extends PluginInterceptorHandler> T getInterceptorHandler(Class<T> clazz){
		PluginInterceptorHandler handler = interceptorHandlers.stream().filter(
		   o -> o.getClass() == clazz
		).findFirst().orElse(null);
	    return (T) handler;
	}
}
