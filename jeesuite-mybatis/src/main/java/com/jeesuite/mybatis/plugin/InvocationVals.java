package com.jeesuite.mybatis.plugin;

import java.util.Collection;
import java.util.Map;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;

import com.jeesuite.common.model.PageParams;
import com.jeesuite.mybatis.plugin.cache.QueryCacheMethodMetadata;
import com.jeesuite.mybatis.plugin.pagination.PageExecutor;
import com.jeesuite.mybatis.plugin.pagination.PaginationHandler;

public class InvocationVals {

	public static final String DOT = ".";
	
	private String concurrentLockKey;
	private Executor executor;
	private Object[] args;
	private MappedStatement mappedStatement;
	private Object parameter;
	private BoundSql boundSql;
	private boolean select;
	private String sql;
	private boolean sqlRewrited;
	
	
	
	private String mapperNameSpace;
	private QueryCacheMethodMetadata queryMethodMetadata;
	private String cacheKey;
	private PageParams pageParam;
	
	public InvocationVals(Invocation invocation) {
		args = invocation.getArgs();
		executor =  (Executor) invocation.getTarget();
		mappedStatement = (MappedStatement)args[0];
		parameter = args[1];
		boundSql = mappedStatement.getBoundSql(parameter);
		//sql = StringUtils.replace(boundSql.getSql(), ";$", StringUtils.EMPTY);
		sql = boundSql.getSql();
		//
		mapperNameSpace = mappedStatement.getId().substring(0, mappedStatement.getId().lastIndexOf(DOT));
		//
		if(select = mappedStatement.getSqlCommandType().equals(SqlCommandType.SELECT)) {
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
		}
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


	public void setQueryCacheMetadata(QueryCacheMethodMetadata queryMethodMetadata,String cacheKey) {
		this.queryMethodMetadata = queryMethodMetadata;
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

	public boolean isSqlRewrited() {
		return sqlRewrited;
	}


}
