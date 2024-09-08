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
package org.dromara.mendmix.mybatis.plugin.pagination;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.async.StandardThreadExecutor;
import org.dromara.mendmix.common.async.StandardThreadExecutor.StandardThreadFactory;
import org.dromara.mendmix.common.model.Page;
import org.dromara.mendmix.common.model.PageParams;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.mybatis.MybatisConfigs;
import org.dromara.mendmix.mybatis.datasource.DatabaseType;
import org.dromara.mendmix.mybatis.exception.MybatisHanlerInitException;
import org.dromara.mendmix.mybatis.kit.MybatisMapperParser;
import org.dromara.mendmix.mybatis.metadata.MapperMetadata;
import org.dromara.mendmix.mybatis.plugin.MendmixMybatisInterceptor;
import org.dromara.mendmix.mybatis.plugin.OnceContextVal;
import org.dromara.mendmix.mybatis.plugin.PluginInterceptorHandler;
import org.dromara.mendmix.mybatis.plugin.rewrite.SqlRewriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * 
 * 
 * <br>
 * Class Name   : PaginationHandler
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年9月05日
 */
public class PaginationHandler implements PluginInterceptorHandler {

	private static Logger logger = LoggerFactory.getLogger(PaginationHandler.class);
	
	private static final String PAGE_QUERY_EXECUTOR_THREAD_PREFIX = "pageQueryExecutor";
	private static final String PAGE_COUNT_SUFFIX = "_PageCount";
	
	public static  Map<String,Boolean> pageMappedStatements = new HashMap<>();
	
	private DatabaseType dbType = DatabaseType.mysql;
	
	private boolean concurrency = ResourceUtils.getBoolean("mendmix-cloud.mybatis.pagination.concurrency");
	private  ThreadPoolExecutor concurrencyQueryExecutor;
	
	@Override
	public void start(MendmixMybatisInterceptor context) {

		this.dbType = DatabaseType.valueOf(MybatisConfigs.getDbType(context.getGroupName()));
		
		logger.info("<startup-logging>  dbType:{}",dbType.name());
		
		List<MapperMetadata> entityInfos = MybatisMapperParser.getMapperMetadatas(context.getGroupName());
		for (MapperMetadata ei : entityInfos) {
			
			Class<?> mapperClass = ei.getMapperClass();
			Method[] methods = mapperClass.getDeclaredMethods();
			for (Method method : methods) {
				if(method.getReturnType() == Page.class){
					String msId = ei.getMapperClass().getName() + "." + method.getName();
					
					boolean withPageParams = false;
					Class<?>[] parameterTypes = method.getParameterTypes();
					self:for (Class<?> clazz : parameterTypes) {
						if(withPageParams = (clazz == PageParams.class || clazz.getSuperclass() == PageParams.class)){
							break self;
						}
					}
					
					if(!withPageParams){
						throw new MybatisHanlerInitException(String.format("method[%s] returnType is:Page,but not found Parameter[PageParams] in Parameters list", method.getName()));
					}
					pageMappedStatements.put(msId,true);
				}
			}
		}
		//
		if(concurrency) {
			final StandardThreadFactory threadFactory = new StandardThreadFactory(PAGE_QUERY_EXECUTOR_THREAD_PREFIX);
			final CallerRunsPolicy rejectedHandler = new CallerRunsPolicy();
			this.concurrencyQueryExecutor = new StandardThreadExecutor(1, 20,60, TimeUnit.SECONDS,20,threadFactory,rejectedHandler);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object onInterceptor(OnceContextVal invocation) throws Throwable {
		
		Page pageObject = invocation.getPageObject();
		if(pageObject == null)return null;
		final MappedStatement orignMappedStatement = invocation.getMappedStatement();
		
		if(!orignMappedStatement.getSqlCommandType().equals(SqlCommandType.SELECT))return null;
		
		if(invocation.getSql() == null) {
			List<Object> list = new ArrayList<>(1);
			list.add(pageObject);
			return list;
		}
		final ResultHandler resultHandler = (ResultHandler) invocation.getArgs() [3];
        //查询总数
        Long total = pageObject.getTotal();
        boolean concurrency = this.concurrency && pageObject.isConcurrency() && total == 0;
        Future<List<?>> dataQueryFuture = null;
        if(total == 0) {
        	if(concurrency) {//异步查询
        		final Map<String, Object> contextVals = CurrentRuntimeContext.getAllContextVals();
        		dataQueryFuture = concurrencyQueryExecutor.submit(new Callable<List<?>>() {
					@Override
					public List<?> call() throws Exception {
						//拒绝策略用了主线程就不处理
						boolean usingSelfThread = Thread.currentThread().getName().startsWith(PAGE_QUERY_EXECUTOR_THREAD_PREFIX);
						if(usingSelfThread) {
							contextVals.forEach((k, v) -> {
								ThreadLocalContext.set(k, v);
							});
						}
						try {
							return executeQuery(invocation, resultHandler);
						} finally {
							if(usingSelfThread) {								
								ThreadLocalContext.unset();
							}
						}
					}
				});
        	}
        	total = executeQueryCount(invocation, resultHandler);
        }
        List<?> data = null;
        if(dataQueryFuture != null) {
        	data = dataQueryFuture.get();
        }else if (total > 0) {
			data = executeQuery(invocation, resultHandler);
		} else {
			data = Lists.newArrayListWithCapacity(0);
		}

        pageObject.setTotal(total);
        pageObject.setData(data);
        pageObject.setOrderBys(null);
		
		List<Page<?>> list = new ArrayList<>(1);
		list.add(pageObject);
		return list;
	}
	
	
	@SuppressWarnings("rawtypes")
	private Long executeQueryCount(OnceContextVal invocation,ResultHandler resultHandler) throws IllegalAccessException, SQLException {
		
		//查询总数
        MappedStatement countMappedStatement = getCountMappedStatement(invocation.getMappedStatement());
        Executor executor = invocation.getExecutor();
        Object parameter = invocation.getParameter();
        BoundSql boundSql = invocation.getBoundSql();
        
		CacheKey countKey = executor.createCacheKey(countMappedStatement, parameter, RowBounds.DEFAULT, boundSql);
		
		// count sql
		String countSql = PageSqlUtils.getCountSql(invocation.getSql());
		
		BoundSql countBoundSql = new BoundSql(countMappedStatement.getConfiguration(), countSql, boundSql.getParameterMappings(),
				parameter);
		//
		SqlRewriteHandler.copyAdditionalParameters(boundSql, countBoundSql);
		// 执行 count 查询
		Object countResultList = executor.query(countMappedStatement, parameter, RowBounds.DEFAULT, resultHandler, countKey,
				countBoundSql);
		try {
			Long count = (Long) ((List) countResultList).get(0);
			return count;
		} catch (IndexOutOfBoundsException e) {
			return 0L;
		}
		
	}
	
	@SuppressWarnings("rawtypes")
	private List executeQuery(OnceContextVal invocation, ResultHandler resultHandler) throws IllegalAccessException, SQLException {
		
		Executor executor = invocation.getExecutor();
		MappedStatement mappedStatement = invocation.getMappedStatement();
		BoundSql boundSql = invocation.getBoundSql();
		Object parameter = invocation.getParameter();
		
		String pageSql = PageSqlUtils.getLimitSQL(dbType,invocation.getSql(),invocation.getPageObject());
		
		BoundSql pageBoundSql = new BoundSql(mappedStatement.getConfiguration(), pageSql, boundSql.getParameterMappings(),
				parameter);
		//
		SqlRewriteHandler.copyAdditionalParameters(boundSql, pageBoundSql);
		
		List<?> resultList = executor.query(mappedStatement, parameter, RowBounds.DEFAULT, resultHandler, null,pageBoundSql);
		return resultList;
	}
	
	/**
     * 新建count查询的MappedStatement
     *
     * @param ms
     * @return
     */
    public MappedStatement getCountMappedStatement(MappedStatement ms) {
    	
    	String newMsId = ms.getId() + PAGE_COUNT_SUFFIX;
    	
    	MappedStatement statement = null;
    	Configuration configuration = ms.getConfiguration();
	
    	try {
    		statement = configuration.getMappedStatement(newMsId);
    		if(statement != null)return statement;
		} catch (Exception e) {}
    	
    	synchronized (configuration) {   
    		
    		if(configuration.hasStatement(newMsId))return configuration.getMappedStatement(newMsId);
    		
    		MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), newMsId, ms.getSqlSource(), ms.getSqlCommandType());
    		builder.resource(ms.getResource());
    		builder.fetchSize(ms.getFetchSize());
    		builder.statementType(ms.getStatementType());
    		builder.keyGenerator(ms.getKeyGenerator());
    		if (ms.getKeyProperties() != null && ms.getKeyProperties().length != 0) {
    			StringBuilder keyProperties = new StringBuilder();
    			for (String keyProperty : ms.getKeyProperties()) {
    				keyProperties.append(keyProperty).append(",");
    			}
    			keyProperties.delete(keyProperties.length() - 1, keyProperties.length());
    			builder.keyProperty(keyProperties.toString());
    		}
    		builder.timeout(ms.getTimeout());
    		builder.parameterMap(ms.getParameterMap());
    		//count查询返回值int
    		 List<ResultMap> resultMaps = new ArrayList<ResultMap>();
             String id = newMsId + "-Inline";
             ResultMap resultMap = new ResultMap.Builder(configuration, id, Long.class, new ArrayList<ResultMapping>(0)).build();
             resultMaps.add(resultMap);
             builder.resultMaps(resultMaps);
             
    		builder.resultSetType(ms.getResultSetType());
    		builder.cache(ms.getCache());
    		builder.flushCacheRequired(ms.isFlushCacheRequired());
    		builder.useCache(ms.isUseCache());
    		
    		statement = builder.build();
    		configuration.addMappedStatement(statement);
    		return statement;
    	}
    	
    }
    

	@Override
	public void onFinished(OnceContextVal invocation, Object result) {
		
	}
	
	@Override
	public void close() {
		if(concurrencyQueryExecutor != null) {
			concurrencyQueryExecutor.shutdown();
		}
	}

	@Override
	public int interceptorOrder() {
		return 3;
	}

}
