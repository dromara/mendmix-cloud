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

import java.util.Collection;
import java.util.Map;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;

import com.mendmix.common.model.PageParams;
import com.mendmix.mybatis.MybatisRuntimeContext;
import com.mendmix.mybatis.crud.CrudMethods;
import com.mendmix.mybatis.metadata.MapperMetadata;
import com.mendmix.mybatis.parser.MybatisMapperParser;
import com.mendmix.mybatis.plugin.cache.QueryCacheMethodMetadata;
import com.mendmix.mybatis.plugin.pagination.PageExecutor;
import com.mendmix.mybatis.plugin.pagination.PaginationHandler;

public class InvocationVals {

	public static final String DOT = ".";
	
	private String concurrentLockKey;
	private Executor executor;
	private Object[] args;
	private MappedStatement mappedStatement;
	private Object parameter;
	private BoundSql boundSql;
	private boolean select;
	private boolean selectByPrimaryKey;
	private String sql;
	private boolean sqlRewrited;
	
	private Map<String, String[]> dataPermValues;
	
	
	
	private String mapperNameSpace;
	private QueryCacheMethodMetadata queryMethodMetadata;
	private String cacheKey;
	private PageParams pageParam;
	
	public InvocationVals(Invocation invocation) {
		args = invocation.getArgs();
		executor =  (Executor) invocation.getTarget();
		mappedStatement = (MappedStatement)args[0];
		parameter = args[1];
		//
		mapperNameSpace = mappedStatement.getId().substring(0, mappedStatement.getId().lastIndexOf(DOT));
		//
		if(select = mappedStatement.getSqlCommandType().equals(SqlCommandType.SELECT)) {
			selectByPrimaryKey = mappedStatement.getId().endsWith(CrudMethods.selectByPrimaryKey.name());
			this.pageParam = PageExecutor.getPageParams();
			if(this.pageParam == null && PaginationHandler.pageMappedStatements.containsKey(mappedStatement.getId())) {
				if(parameter instanceof Map){
	        		Collection<?> parameterValues = ((Map<?,?>)parameter).values();
	        		for (Object val : parameterValues) {
						if(val instanceof PageParams){
							this.pageParam = (PageParams) val;
							break;
						}
					}
	        	}else{
	        		this.pageParam = (PageParams) parameter;
	        	}
			}
			//
			if(mappedStatement.getId().endsWith(CrudMethods.countByExample.name()) 
					|| mappedStatement.getId().endsWith(CrudMethods.selectByExample.name())) {
				MapperMetadata mapperMetadata = MybatisMapperParser.getMapperMetadata(mapperNameSpace);
				MybatisRuntimeContext.setMapperMetadata(mapperMetadata);
			}
		}
		boundSql = mappedStatement.getBoundSql(parameter);
		sql = boundSql.getSql();
	}
	
	public Executor getExecutor() {
		return executor;
	}

	public String getConcurrentLockKey() {
		return concurrentLockKey;
	}


	public void setConcurrentLockKey(String concurrentLockKey) {
		this.concurrentLockKey = concurrentLockKey;
	}

	public boolean isSelect() {
		return select;
	}

	public String getSql() {
		return sql;
	}


	public void setRewriteSql(String sql) {
		this.sql = sql;
		this.sqlRewrited = true;
	}


	public Object[] getArgs() {
		return args;
	}


	public MappedStatement getMappedStatement() {
		return mappedStatement;
	}


	public Object getParameter() {
		return parameter;
	}


	public BoundSql getBoundSql() {
		return boundSql;
	}


	public QueryCacheMethodMetadata getQueryMethodMetadata() {
		return queryMethodMetadata;
	}


	public void setQueryCacheMetadata(QueryCacheMethodMetadata queryMethodMetadata) {
		this.queryMethodMetadata = queryMethodMetadata;
	}
	

	public void setCacheKey(String cacheKey) {
		this.cacheKey = cacheKey;
	}

	public String getMapperNameSpace() {
		return mapperNameSpace;
	}


	public String getCacheKey() {
		return cacheKey;
	}

	public PageParams getPageParam() {
		return pageParam;
	}
	

	public boolean isSelectByPrimaryKey() {
		return selectByPrimaryKey;
	}

	public boolean isSqlRewrited() {
		return sqlRewrited;
	}

	public Map<String, String[]> getDataPermValues() {
		return dataPermValues;
	}


	public void setDataPermValues(Map<String, String[]> dataPermValues) {
		this.dataPermValues = dataPermValues;
	}


	public boolean isDynaDataPermEnaled() {
		return dataPermValues != null;
	}

}
