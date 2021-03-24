package com.jeesuite.mybatis.plugin.dataprofile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import com.jeesuite.common.model.Page;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.mybatis.MybatisRuntimeContext;
import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.parser.EntityInfo;
import com.jeesuite.mybatis.parser.MybatisMapperParser;
import com.jeesuite.mybatis.plugin.InvocationVals;
import com.jeesuite.mybatis.plugin.JeesuiteMybatisInterceptor;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

/**
 * 数据权限mybatis层拦截处理器 <br>
 * Class Name : DataProfileHandler
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年10月28日
 */
public class DataProfileHandler implements InterceptorHandler {

	private final static Logger logger = LoggerFactory.getLogger("com.jeesuite.mybatis.plugin.dataprofile");
    
	public static final String NAME = "dataProfile";
	
	private Set<String> includeMapperIds = new HashSet<>();
	private List<String> excludeMapperIds;
	private Map<String, Map<String,String>> dataProfileMappings = new HashMap<>();
	
	@Override
	public void start(JeesuiteMybatisInterceptor context) {
		
		Properties properties = ResourceUtils.getAllProperties("jeesuite.mybatis.dataProfile.mappings");
		properties.forEach( (k,v) -> {
			String tableName = k.toString().substring(k.toString().indexOf("[") + 1).replace("]", "").trim();
			buildTableDataProfileMapping(tableName, v.toString());
		} );
		
        //
		Set<String> tableNames = dataProfileMappings.keySet();
		List<EntityInfo> entityInfos = MybatisMapperParser.getEntityInfos(context.getGroupName());
		for (EntityInfo entityInfo : entityInfos) {
			entityInfo.getMapperSqls().forEach( (k,v) -> {
				for (String tableName : tableNames) {
					if(v.contains(tableName)) {
						//entityInfo.getMapperMethod(k)
						includeMapperIds.add(k);
					}
				}
			} );
		}
		
		String[] excludeMapperIdArray = org.springframework.util.StringUtils.tokenizeToStringArray(ResourceUtils.getProperty("jeesuite.mybatis.dataProfile.excludeMapperIds",""), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
		excludeMapperIds = Arrays.asList(excludeMapperIdArray);
				
		logger.info("dataProfileMappings >> {}",dataProfileMappings);
	}

	@Override
	public Object onInterceptor(InvocationVals invocation) throws Throwable {
		if(!invocation.isSelect())return null;
		//
		if(MybatisRuntimeContext.isTransactionalOn())return null;
		if(MybatisRuntimeContext.isDataProfileIgnore())return null;
		
		final Executor executor = invocation.getExecutor();
		final Object[] args = invocation.getArgs();
		final MappedStatement mappedStatement = invocation.getMappedStatement();
		
		if(!includeMapperIds.contains(mappedStatement.getId()))return null;
		if(excludeMapperIds.contains(mappedStatement.getId()))return null;

		Map<String, String[]> dataMappings = MybatisRuntimeContext.getDataProfileMappings();
		if(dataMappings == null)return null;
		
		BoundSql boundSql = invocation.getBoundSql();
		String newSql = buildDataProfileSql(mappedStatement.getId(),invocation.getSql(),dataMappings);
		//直接返回
		if(newSql == null) {
			List<Object> list = new ArrayList<>(1);
			//
			EntityInfo entityInfo = MybatisMapperParser.getEntityInfoByMapper(invocation.getMapperNameSpace());
			String methodName = mappedStatement.getId().replace(invocation.getMapperNameSpace(), StringUtils.EMPTY).substring(1);
			Class<?> returnType = entityInfo.getMapperMethod(methodName).getMethod().getReturnType();
			
			if(returnType == int.class || returnType == Integer.class|| returnType == long.class|| returnType == Long.class) {
				list.add(0);
			}else if(invocation.getPageParam() != null) {
				list.add(new Page<Object>(invocation.getPageParam(),0,null));
			}
			
			return list;
		}
		//如果是分页查询，直接返回重写后的sql，有分页查询处理查询逻辑
		if(invocation.getPageParam() != null) {
			invocation.setSql(newSql);
			return null;
		}
		
		final ResultHandler<?> resultHandler = (ResultHandler<?>) args[3];
		CacheKey cacheKey = executor.createCacheKey(mappedStatement, invocation.getParameter(), RowBounds.DEFAULT, boundSql);
		BoundSql newBoundSql = new BoundSql(mappedStatement.getConfiguration(), newSql,boundSql.getParameterMappings(), invocation.getParameter());

		List<?> resultList = executor.query(mappedStatement, invocation.getParameter(), RowBounds.DEFAULT, resultHandler, cacheKey,newBoundSql);

		return resultList;
	}

	/**
	 * @param mappedStatementId
	 * @param orignSql
	 * @param dataMappings
	 * @return
	 */
	private String buildDataProfileSql(String mappedStatementId,String orignSql, Map<String, String[]> dataMapping) {
		Select select = null;
		try {
			select = (Select) CCJSqlParserUtil.parse(orignSql);
		} catch (JSQLParserException e) {
			logger.error("rebuildDataProfileSql_ERROR",e);
			throw new RuntimeException("sql解析错误");
		}
		
		PlainSelect selectBody = (PlainSelect) select.getSelectBody();
		Table table = (Table) selectBody.getFromItem();
		
		Map<String, String> columnMapping;
		Set<String> fieldNames;
		Expression newExpression = null;
		String[] values;
		if(dataProfileMappings.containsKey(table.getName())) {
			columnMapping = dataProfileMappings.get(table.getName());
			fieldNames = columnMapping.keySet();
			for (String fieldName : fieldNames) {
				if(!dataMapping.containsKey(fieldName))continue;
				values = dataMapping.get(fieldName);
				//如果某个匹配字段为空直接返回null，不在查询数据库
				if(values == null || values.length == 0) {
					return null;
				}
				newExpression = appendDataProfileCondition(table, selectBody.getWhere(), columnMapping.get(fieldName),values);
				selectBody.setWhere(newExpression);
				//TODO 主表已经处理的条件，join表不在处理
			}
		}
		
		//JOIN 
		List<Join> joins = selectBody.getJoins();
		if(joins != null){
			for (Join join : joins) {
				table = (Table) join.getRightItem();
				if(dataProfileMappings.containsKey(table.getName())) {
					columnMapping = dataProfileMappings.get(table.getName());
					fieldNames = columnMapping.keySet();
					for (String fieldName : fieldNames) {
						if(!dataMapping.containsKey(fieldName))continue;
						values = dataMapping.get(fieldName);
						//如果某个匹配字段为空直接返回null，不在查询数据库
						if(values == null || values.length == 0) {
							return null;
						}
						//左右连接加在ON 无法过滤
						//newExpression = appendDataProfileCondition(table, join.getOnExpression(), columnMapping.get(fieldName),dataMapping.get(fieldName));
						//join.setOnExpression(newExpression);
						newExpression = appendDataProfileCondition(table, selectBody.getWhere(), columnMapping.get(fieldName),values);
						selectBody.setWhere(newExpression);
					}
				}
			}
		}
		//
		String newSql = selectBody.toString();
		
		return newSql;
	}
	
	private static Expression appendDataProfileCondition(Table table,Expression orginExpression,String columnName,String[] values){
		Expression newExpression = null;
		Column column = new Column(table, columnName);
		if (values.length == 1) {
			EqualsTo equalsTo = new EqualsTo();
			equalsTo.setLeftExpression(column);
			equalsTo.setRightExpression(new StringValue(values[0]));
			newExpression = orginExpression == null ? equalsTo : new AndExpression(orginExpression, equalsTo);
		} else {
			ExpressionList expressionList = new ExpressionList(new ArrayList<>(values.length));
			for (String value : values) {
				expressionList.getExpressions().add(new StringValue(value));
			}
			InExpression inExpression = new InExpression(column, expressionList);
			newExpression = orginExpression == null ? inExpression : new AndExpression(orginExpression,inExpression);
		}
		
		return newExpression;
	}
	
	private void buildTableDataProfileMapping(String tableName,String ruleString) {
		dataProfileMappings.put(tableName, new HashMap<>());
		String[] rules = ruleString.split(",|;");
		String[] tmpArr;
		for (String rule : rules) {
			tmpArr = rule.split(":");
			String columnName = tmpArr.length == 2 ? tmpArr[1] : rule;
			dataProfileMappings.get(tableName).put(tmpArr[0], columnName);
		}
	}

	@Override
	public void onFinished(InvocationVals invocation, Object result) {
	}

	@Override
	public int interceptorOrder() {
		//需要在分页前执行
		return 2;
	}

	@Override
	public void close() {
	}

}
