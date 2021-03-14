package com.jeesuite.mybatis.plugin.dataprofile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.mybatis.MybatisRuntimeContext;
import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.crud.helper.ColumnMapper;
import com.jeesuite.mybatis.crud.helper.EntityHelper;
import com.jeesuite.mybatis.crud.helper.EntityMapper;
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
	
	private List<String> excludeMapperIds;
	private Map<String, List<ColumnMapper>> dataProfileMappings = new HashMap<>();
	
	@Override
	public void start(JeesuiteMybatisInterceptor context) {
		List<String> fieldNames = Arrays.asList(ResourceUtils.getAndValidateProperty("jeesuite.mybatis.dataProfile.fieldNames").split(",|;"));
		String[] excludeMapperIdArray = org.springframework.util.StringUtils.tokenizeToStringArray(ResourceUtils.getProperty("jeesuite.mybatis.dataProfile.excludeMapperIds",""), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
		excludeMapperIds = Arrays.asList(excludeMapperIdArray);
		
		List<EntityInfo> entityInfos = MybatisMapperParser.getEntityInfos(context.getGroupName());
		
		List<ColumnMapper> columns;
		for (EntityInfo entityInfo : entityInfos) {
			EntityMapper entityMapper = EntityHelper.getEntityMapper(entityInfo.getEntityClass());
			for (ColumnMapper columnMapper : entityMapper.getColumnsMapper()) {
				if(fieldNames.contains(columnMapper.getProperty())){
					columns = dataProfileMappings.get(entityInfo.getMapperClass().getName());
					if(columns == null){
						columns = new ArrayList<>(fieldNames.size());
						dataProfileMappings.put(entityInfo.getMapperClass().getName(), columns);
					}
					columns.add(columnMapper);
				}
			}
		}
		
		logger.info("dataProfileMappings >> {}",dataProfileMappings);
	}

	@Override
	public Object onInterceptor(InvocationVals invocation) throws Throwable {
		if(!invocation.isSelect())return null;
		final Executor executor = invocation.getExecutor();
		final Object[] args = invocation.getArgs();
		final MappedStatement mappedStatement = invocation.getMappedStatement();
		
		if(excludeMapperIds.contains(mappedStatement.getId()))return null;

		Map<String, String[]> dataMappings = MybatisRuntimeContext.getDataProfileMappings();
		if(dataMappings == null)return null;
		
		String mapperClassName = mappedStatement.getId().substring(0, mappedStatement.getId().lastIndexOf("."));
		if(!dataProfileMappings.containsKey(mapperClassName))return null;

		BoundSql boundSql = invocation.getBoundSql();
		String newSql = buildDataProfileSql(invocation.getSql(),new ArrayList<>(dataProfileMappings.get(mapperClassName)),dataMappings);
		//如果是分页查询，直接返回重写后的sql，有分页查询处理查询逻辑
		if(invocation.getPageParam() != null) {
			invocation.setSql(newSql);
			return null;
		}
		
		final ResultHandler<?> resultHandler = (ResultHandler<?>) args[3];
		BoundSql newBoundSql = new BoundSql(mappedStatement.getConfiguration(), newSql,
				boundSql.getParameterMappings(), invocation.getParameter());

		List<?> resultList = executor.query(mappedStatement, invocation.getParameter(), RowBounds.DEFAULT, resultHandler, null,
				newBoundSql);

		return resultList;
	}

	/**
	 * @param orignSql
	 * @param columnDefineList
	 * @param dataMappings
	 * @return
	 */
	private String buildDataProfileSql(String orignSql, List<ColumnMapper> columnDefineList,Map<String, String[]> dataMapping) {
		Select select = null;
		try {
			select = (Select) CCJSqlParserUtil.parse(orignSql);
		} catch (JSQLParserException e) {
			logger.error("rebuildDataProfileSql_ERROR",e);
			throw new RuntimeException("sql解析错误");
		}
		
		PlainSelect selectBody = (PlainSelect) select.getSelectBody();
		Table table = (Table) selectBody.getFromItem();
		
		Iterator<ColumnMapper> iterator = columnDefineList.iterator();
		Expression newExpression = null;
		ColumnMapper item;
		while(iterator.hasNext()){
			item =iterator.next();
			if(!dataMapping.containsKey(item.getProperty()))continue;
			newExpression = appendDataProfileCondition(table, selectBody.getWhere(), item.getColumn(),dataMapping.get(item.getProperty()));
			selectBody.setWhere(newExpression);
			//主表已经处理的条件，join表不在处理
			iterator.remove();
		}
		
		//JOIN 
		List<Join> joins = selectBody.getJoins();
		if(joins != null && !columnDefineList.isEmpty()){
			List<ColumnMapper> columns;
			for (Join join : joins) {
				table = (Table) join.getRightItem();
				columns = EntityHelper.getTableColumnMappers(table.getName());
				if(columns == null)continue;
				for (ColumnMapper defineColumn : columnDefineList) {
					if(!columns.contains(defineColumn))continue;
					if(!dataMapping.containsKey(defineColumn.getProperty()))continue;
					newExpression = appendDataProfileCondition(table, join.getOnExpression(), defineColumn.getColumn(),dataMapping.get(defineColumn.getProperty()));
					join.setOnExpression(newExpression);
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
