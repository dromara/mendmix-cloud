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

import java.util.Collection;
import java.util.Map;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.dromara.mendmix.common.model.Page;
import org.dromara.mendmix.common.model.PageParams;
import org.dromara.mendmix.mybatis.MybatisRuntimeContext;
import org.dromara.mendmix.mybatis.crud.CrudMethods;
import org.dromara.mendmix.mybatis.kit.MybatisMapperParser;
import org.dromara.mendmix.mybatis.metadata.MapperMetadata;
import org.dromara.mendmix.mybatis.plugin.cache.QueryCacheMethodMetadata;
import org.dromara.mendmix.mybatis.plugin.pagination.PageExecutor;
import org.dromara.mendmix.mybatis.plugin.pagination.PaginationHandler;

public class OnceContextVal {

	public static final String DOT = ".";
	
	private String groupName;
	private String concurrentLockKey;
	private Executor executor;
	private Object[] args;
	private MappedStatement mappedStatement;
	private boolean selectByPrimaryKey;
	private MapperMetadata entityInfo;
	private Object parameter;
	private BoundSql boundSql;
	private boolean select;
	private String sql;
	private boolean sqlRewrited;
	private boolean usingDataPermission;
	
	private String mapperNameSpace;
	private QueryCacheMethodMetadata queryMethodMetadata;
	private String cacheKey;
	private Page<?> pageObject;
	
	Map<String, String> tableNameMapping;
	
	public OnceContextVal(String groupName,Invocation invocation) {
		this.groupName = groupName;
		args = invocation.getArgs();
		executor =  (Executor) invocation.getTarget();
		mappedStatement = (MappedStatement)args[0];
		parameter = args[1];
		boundSql = mappedStatement.getBoundSql(parameter);
		//sql = StringUtils.replace(boundSql.getSql(), ";$", StringUtils.EMPTY);
		sql = boundSql.getSql();
		//
		mapperNameSpace = mappedStatement.getId().substring(0, mappedStatement.getId().lastIndexOf(DOT));
		entityInfo = MybatisMapperParser.getMapperMetadata(mapperNameSpace);
		//
		if(select = mappedStatement.getSqlCommandType().equals(SqlCommandType.SELECT)) {
			selectByPrimaryKey = mappedStatement.getId().endsWith(CrudMethods.selectByPrimaryKey.name());
			this.pageObject = PageExecutor.getPageObject();
			if(this.pageObject == null && PaginationHandler.pageMappedStatements.containsKey(mappedStatement.getId())) {
				if(parameter instanceof Map){
	        		Collection<?> parameterValues = ((Map<?,?>)parameter).values();
	        		for (Object val : parameterValues) {
						if(val instanceof PageParams){
							this.pageObject = new Page<>((PageParams) val, 0, null);
							break;
						}
					}
	        	}else{
	        		this.pageObject = new Page<>((PageParams) parameter, 0, null);
	        	}
			}
		}
		//
		tableNameMapping = MybatisRuntimeContext.getRewriteTableNameRules();
		//
		MybatisRuntimeContext.setOnceContextVal(this);
	}
	
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getGroupName() {
		return groupName;
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

	public MapperMetadata getEntityInfo() {
		return entityInfo;
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

	public boolean isSelectByPrimaryKey() {
		return selectByPrimaryKey;
	}

	public String getCacheKey() {
		return cacheKey;
	}

	public Page<?> getPageObject() {
		return pageObject;
	}

	public void setPageObject(Page<?> pageObject) {
		this.pageObject = pageObject;
	}

	public boolean isSqlRewrited() {
		return sqlRewrited;
	}
	
	public Map<String, String> getTableNameMapping() {
		return tableNameMapping;
	}

	public void setTableNameMapping(Map<String, String> tableNameMapping) {
		this.tableNameMapping = tableNameMapping;
	}

	public boolean isUsingDataPermission() {
		return usingDataPermission;
	}

	public void setUsingDataPermission(boolean usingDataPermission) {
		this.usingDataPermission = usingDataPermission;
	}
    

}
