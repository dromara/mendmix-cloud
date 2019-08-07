package com.jeesuite.mybatis.plugin.pagination;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.mybatis.MybatisConfigs;
import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.exception.MybatisHanlerInitException;
import com.jeesuite.mybatis.parser.EntityInfo;
import com.jeesuite.mybatis.parser.MybatisMapperParser;
import com.jeesuite.mybatis.plugin.JeesuiteMybatisInterceptor;
import com.jeesuite.mybatis.plugin.pagination.PageSqlUtils.DbType;
import com.jeesuite.mybatis.plugin.pagination.annotation.Pageable;

public class PaginationHandler implements InterceptorHandler {

	private static Logger logger = LoggerFactory.getLogger(PaginationHandler.class);
	
	public static final String NAME = "page";
	
	private static final String PAGE_COUNT_SUFFIX = "_PageCount";
	
	private Map<String,Boolean> pageMappedStatements = new HashMap<>();
	
	private DbType dbType = DbType.MYSQL;
	
	public void setDbType(String dbType){
		if(StringUtils.isBlank(dbType))return;
		DbType[] dbTypes = DbType.values();
		for (DbType dt : dbTypes) {
			if(dt.name().equalsIgnoreCase(dbType)){
				this.dbType = dt;
				break;
			}
		}
	}

	@Override
	public void start(JeesuiteMybatisInterceptor context) {

		setDbType(MybatisConfigs.getDbType(context.getGroupName()));
		
		logger.info("dbType:{}",dbType.name());
		
		List<EntityInfo> entityInfos = MybatisMapperParser.getEntityInfos(context.getGroupName());
		for (EntityInfo ei : entityInfos) {
			
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
				}else if(method.isAnnotationPresent(Pageable.class)){
					String msId = ei.getMapperClass().getName() + "." + method.getName();
					pageMappedStatements.put(msId,true);
				}
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object onInterceptor(Invocation invocation) throws Throwable {
		try {
			final Executor executor = (Executor) invocation.getTarget();
			final Object[] args = invocation.getArgs();
			final MappedStatement orignMappedStatement = (MappedStatement)args[0];
			
			if(!orignMappedStatement.getSqlCommandType().equals(SqlCommandType.SELECT))return null;
			
			PageParams pageParams = PageExecutor.getPageParams();
			if(pageParams == null && !pageMappedStatements.keySet().contains(orignMappedStatement.getId()))return null;
			
			final RowBounds rowBounds = (RowBounds) args[2];
			final ResultHandler resultHandler = (ResultHandler) args[3];
	        final Object parameter = args[1];
	        
	        BoundSql boundSql;
	        if(args.length == 4){
	            boundSql = orignMappedStatement.getBoundSql(parameter);
	        } else {
	            boundSql = (BoundSql) args[5];
	        }
	        
	        if(pageParams == null && pageMappedStatements.get(orignMappedStatement.getId())){
	        	if(parameter instanceof Map){
	        		Collection parameterValues = ((Map)parameter).values();
	        		for (Object val : parameterValues) {
						if(val instanceof PageParams){
							pageParams = (PageParams) val;
							break;
						}
					}
	        	}else{
	        		pageParams = (PageParams) parameter;
	        	}
	        }
	        
	        if(pageParams == null)return null;
	        
	        //查询总数
	        MappedStatement countMappedStatement = getCountMappedStatement(orignMappedStatement);
	        Long total = executeQueryCount(executor, countMappedStatement, parameter, boundSql, rowBounds, resultHandler);
	        //查询分页数据
	        List<?> datas = executeQuery(executor, orignMappedStatement, parameter, boundSql, rowBounds, resultHandler, pageParams);	        

	        Page<Object> page = new Page<Object>(pageParams,total,(List<Object>) datas);	
			
			List<Page<?>> list = new ArrayList<Page<?>>(1);
			list.add(page);
			return list;
		} finally {
			PageExecutor.clearPageParams();
		}
	}
	
	
	@SuppressWarnings("rawtypes")
	private Long executeQueryCount(Executor executor, MappedStatement countMs,
            Object parameter, BoundSql boundSql,
			RowBounds rowBounds, ResultHandler resultHandler) throws IllegalAccessException, SQLException {
		CacheKey countKey = executor.createCacheKey(countMs, parameter, RowBounds.DEFAULT, boundSql);
		
		String orignSql = StringUtils.replace(boundSql.getSql(), ";$", StringUtils.EMPTY);
		// count sql
		String countSql = PageSqlUtils.getCountSql(orignSql);
		
		BoundSql countBoundSql = new BoundSql(countMs.getConfiguration(), countSql, boundSql.getParameterMappings(),
				parameter);
//		for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
//			String propertyName = parameterMapping.getProperty();
//			if(boundSql.hasAdditionalParameter(propertyName)){
//				countBoundSql.setAdditionalParameter(propertyName, boundSql.getAdditionalParameter(propertyName));
//			}
//		}
		// 执行 count 查询
		Object countResultList = executor.query(countMs, parameter, RowBounds.DEFAULT, resultHandler, countKey,
				countBoundSql);
		Long count = (Long) ((List) countResultList).get(0);
		return count;
	}
	
	@SuppressWarnings("rawtypes")
	private List executeQuery(Executor executor, MappedStatement ms,
            Object parameter, BoundSql boundSql,
			RowBounds rowBounds, ResultHandler resultHandler,PageParams pageParams) throws IllegalAccessException, SQLException {
		CacheKey countKey = executor.createCacheKey(ms, parameter, RowBounds.DEFAULT, boundSql);
		
		String orignSql = StringUtils.replace(boundSql.getSql(), ";$", StringUtils.EMPTY);
		
		String pageSql = PageSqlUtils.getLimitSQL(dbType,orignSql,pageParams);
		
		BoundSql countBoundSql = new BoundSql(ms.getConfiguration(), pageSql, boundSql.getParameterMappings(),
				parameter);
		
		List<?> resultList = executor.query(ms, parameter, RowBounds.DEFAULT, resultHandler, countKey,
				countBoundSql);
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
	public void onFinished(Invocation invocation, Object result) {
		
	}
	
	@Override
	public void close() {}

	@Override
	public int interceptorOrder() {
		return 9;
	}

}
