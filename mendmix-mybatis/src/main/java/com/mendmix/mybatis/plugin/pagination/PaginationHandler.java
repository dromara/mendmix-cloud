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
package com.mendmix.mybatis.plugin.pagination;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.common.model.Page;
import com.mendmix.common.model.PageParams;
import com.mendmix.mybatis.MybatisConfigs;
import com.mendmix.mybatis.core.InterceptorHandler;
import com.mendmix.mybatis.datasource.DatabaseType;
import com.mendmix.mybatis.exception.MybatisHanlerInitException;
import com.mendmix.mybatis.metadata.MapperMetadata;
import com.mendmix.mybatis.parser.MybatisMapperParser;
import com.mendmix.mybatis.plugin.InvocationVals;
import com.mendmix.mybatis.plugin.JeesuiteMybatisInterceptor;
import com.mendmix.mybatis.plugin.rewrite.SqlRewriteHandler;

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
public class PaginationHandler implements InterceptorHandler {

	private static Logger logger = LoggerFactory.getLogger(PaginationHandler.class);
	
	private static final String PAGE_COUNT_SUFFIX = "_PageCount";
	
	public static  Map<String,Boolean> pageMappedStatements = new HashMap<>();
	
	private DatabaseType dbType = DatabaseType.mysql;

	@Override
	public void start(JeesuiteMybatisInterceptor context) {

		this.dbType = DatabaseType.valueOf(MybatisConfigs.getDbType(context.getGroupName()));
		
		logger.info("dbType:{}",dbType.name());
		
		List<MapperMetadata> mappers = MybatisMapperParser.getMapperMetadatas(context.getGroupName());
		for (MapperMetadata e : mappers) {
			
			Class<?> mapperClass = e.getMapperClass();
			Method[] methods = mapperClass.getDeclaredMethods();
			for (Method method : methods) {
				if(method.getReturnType() == Page.class){
					String msId = e.getMapperClass().getName() + "." + method.getName();
					
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
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object onInterceptor(InvocationVals invocation) throws Throwable {
		
		PageParams pageParams = invocation.getPageParam();
		if(pageParams == null)return null;
		final MappedStatement orignMappedStatement = invocation.getMappedStatement();
		
		if(!orignMappedStatement.getSqlCommandType().equals(SqlCommandType.SELECT))return null;
		
		if(invocation.getSql() == null) {
			List<Object> list = new ArrayList<>(1);
			list.add(new Page<Object>(invocation.getPageParam(),0,null));
			return list;
		}
		final ResultHandler resultHandler = (ResultHandler) invocation.getArgs() [3];
        //查询总数
        Long total = executeQueryCount(invocation, resultHandler);
        //查询分页数据
        List<?> datas = executeQuery(invocation, resultHandler);	        

        Page<Object> page = new Page<Object>(pageParams,total,(List<Object>) datas);	
		
		List<Page<?>> list = new ArrayList<Page<?>>(1);
		list.add(page);
		return list;
	}
	
	
	@SuppressWarnings("rawtypes")
	private Long executeQueryCount(InvocationVals invocation,ResultHandler resultHandler) throws IllegalAccessException, SQLException {
		
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
		SqlRewriteHandler.copyForeachAdditionlParams(boundSql, countBoundSql);
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
	private List executeQuery(InvocationVals invocation, ResultHandler resultHandler) throws IllegalAccessException, SQLException {
		
		Executor executor = invocation.getExecutor();
		MappedStatement mappedStatement = invocation.getMappedStatement();
		BoundSql boundSql = invocation.getBoundSql();
		Object parameter = invocation.getParameter();
		
		String pageSql = PageSqlUtils.getLimitSQL(dbType,invocation.getSql(),invocation.getPageParam());
		
		BoundSql pageBoundSql = new BoundSql(mappedStatement.getConfiguration(), pageSql, boundSql.getParameterMappings(),
				parameter);
		//
		SqlRewriteHandler.copyForeachAdditionlParams(boundSql, pageBoundSql);
		
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
	public void onFinished(InvocationVals invocation, Object result) {
		
	}
	
	@Override
	public void close() {}

	@Override
	public int interceptorOrder() {
		return 3;
	}

}
