/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.mybatis.plugin;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import com.mendmix.mybatis.core.InterceptorHandler;
import com.mendmix.mybatis.plugin.autofield.AutoFieldFillHandler;
import com.mendmix.mybatis.plugin.cache.CacheHandler;
import com.mendmix.mybatis.plugin.pagination.PaginationHandler;
import com.mendmix.mybatis.plugin.rewrite.SqlRewriteHandler;
import com.mendmix.mybatis.plugin.rwseparate.RwRouteHandler;

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

	protected static final Logger logger = LoggerFactory.getLogger("com.mendmix.mybatis");
	
	private String groupName;
	private List<InterceptorHandler> interceptorHandlers = new ArrayList<>();
	
	private static boolean cacheEnabled,rwRouteEnabled;
	
	public MendmixMybatisInterceptor(String groupName, String[] hanlderNames) {
		this.groupName = groupName;
		//
		this.interceptorHandlers.add(new SqlRewriteHandler());
		this.interceptorHandlers.add(new AutoFieldFillHandler());
		this.interceptorHandlers.add(new PaginationHandler());
		
		this.setInterceptorHandlers(hanlderNames);
	}

	private void setInterceptorHandlers(String[] hanlderNames) {
		for (String name : hanlderNames) {
			if(org.apache.commons.lang3.StringUtils.isBlank(name))continue;
			if(CacheHandler.NAME.equals(name)){
				this.interceptorHandlers.add(new CacheHandler());
				cacheEnabled = true;
			}else if(RwRouteHandler.NAME.equals(name)){
				this.interceptorHandlers.add(new RwRouteHandler());
				rwRouteEnabled = true;
			}else{
				//自定义的拦截器
				try {
					Class<?> clazz = Class.forName(name);
					InterceptorHandler handler = (InterceptorHandler) clazz.newInstance();
					this.interceptorHandlers.add(handler);
					logger.info("MENDMIX-TRACE-LOGGGING-->> registered cumstom InterceptorHandler:{}",name);
				} catch (Exception e) {
					logger.error("MENDMIX-TRACE-LOGGGING-->> registered cumstom InterceptorHandler error",e);
				}
			}
		}
		
		//排序 分页和数据权限会重写sql，所以不走缓存，所以放在缓存之前
		Collections.sort(this.interceptorHandlers, new Comparator<InterceptorHandler>() {
			@Override
			public int compare(InterceptorHandler o1, InterceptorHandler o2) {
				return Integer.compare(o1.interceptorOrder(), o2.interceptorOrder());
			}
		});
		
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		
		InvocationVals invocationVal = new InvocationVals(invocation);
		
		Object result = null;
		boolean cacheHited = false;
		try {
			for (InterceptorHandler handler : interceptorHandlers) {
				result = handler.onInterceptor(invocationVal);
				if(result != null) {
					cacheHited = handler.getClass() == CacheHandler.class;
					break;
				}
			}
			
			if(result == null){
				result = invocation.proceed();
			}

			return result;
		} finally {
			for (InterceptorHandler handler : interceptorHandlers) {
				if(cacheHited && handler.getClass() == CacheHandler.class)continue;
				try {					
					handler.onFinished(invocationVal,result);
				} catch (Exception e) {
					e.printStackTrace();
				}
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
	public void setProperties(Properties properties) {}


	public void afterRegister()  {
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
	
	public static boolean isCacheEnabled() {
		return cacheEnabled;
	}

	public static boolean isRwRouteEnabled() {
		return rwRouteEnabled;
	}
	
	public String getGroupName() {
		return groupName;
	}
}
