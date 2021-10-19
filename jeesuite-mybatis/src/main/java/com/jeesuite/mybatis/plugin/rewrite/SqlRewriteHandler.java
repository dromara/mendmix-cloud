package com.jeesuite.mybatis.plugin.rewrite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.model.OrderBy;
import com.jeesuite.common.model.OrderBy.OrderType;
import com.jeesuite.common.model.PageParams;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.mybatis.MybatisConfigs;
import com.jeesuite.mybatis.MybatisRuntimeContext;
import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.crud.helper.ColumnMapper;
import com.jeesuite.mybatis.crud.helper.EntityHelper;
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
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

/**
 * sql重写处理器 <br>
 * Class Name : SqlRewriteHandler
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年10月28日
 */
public class SqlRewriteHandler implements InterceptorHandler {

	private final static Logger logger = LoggerFactory.getLogger("com.jeesuite.mybatis.plugin");

	public static final String TENANT_ID = "tenantId";
	private static final String FRCH_INDEX_PREFIX = "_frch_index_";
	private static final String FRCH_ITEM_PREFIX = "__frch_item_";
	
	private Map<String, Map<String,String>> dataProfileMappings = new HashMap<>();
	
	private boolean isFieldSharddingTenant;

	@Override
	public void start(JeesuiteMybatisInterceptor context) {
	
		isFieldSharddingTenant = MybatisConfigs.isFieldSharddingTenant(context.getGroupName());
		
		Properties properties = ResourceUtils.getAllProperties("jeesuite.mybatis.dataPermission.mappings");
		properties.forEach( (k,v) -> {
			String tableName = k.toString().substring(k.toString().indexOf("[") + 1).replace("]", "").trim();
			buildTableDataProfileMapping(tableName, v.toString());
		} );
		
		final List<EntityInfo> entityInfos = MybatisMapperParser.getEntityInfos(context.getGroupName());
		//字段隔离租户模式
		if (isFieldSharddingTenant) {
			String tenantField = MybatisConfigs.getTenantSharddingField(context.getGroupName());

			ColumnMapper tenantColumn;
			for (EntityInfo entityInfo : entityInfos) {
				tenantColumn = EntityHelper.getTableColumnMappers(entityInfo.getTableName()).stream().filter(o -> {
					return o.getColumn().equals(tenantField) || o.getProperty().equals(tenantField);
				}).findFirst().orElse(null);

				if (tenantColumn == null)
					continue;

				if (!dataProfileMappings.containsKey(entityInfo.getTableName())) {
					dataProfileMappings.put(entityInfo.getTableName(), new HashMap<>());
				}
				dataProfileMappings.get(entityInfo.getTableName()).put(TENANT_ID, tenantColumn.getColumn());
			}
		}
		
		logger.info("dataProfileMappings >> {}",dataProfileMappings);
	}

	@Override
	public Object onInterceptor(InvocationVals invocation) throws Throwable {
		if(!invocation.isSelect())return null;
		Map<String, String[]> dataMappings = MybatisRuntimeContext.getDataProfileMappings();
		//
		rewriteSql(invocation, dataMappings);
		
		if(invocation.getPageParam() != null)return null;
        //不查数据库直接返回
		if(invocation.getSql() == null) {
			List<Object> list = new ArrayList<>(1);
			//
			EntityInfo entityInfo = MybatisMapperParser.getEntityInfoByMapper(invocation.getMapperNameSpace());
			String methodName = invocation.getMappedStatement().getId().replace(invocation.getMapperNameSpace(), StringUtils.EMPTY).substring(1);
			Class<?> returnType = entityInfo.getMapperMethod(methodName).getMethod().getReturnType();
			if(returnType == int.class || returnType == Integer.class|| returnType == long.class|| returnType == Long.class) {
				list.add(0);
			}
			
			return list;
		}else {
			Executor executor = invocation.getExecutor();
			MappedStatement mappedStatement = invocation.getMappedStatement();
			ResultHandler<?> resultHandler = (ResultHandler<?>) invocation.getArgs()[3];
			List<ParameterMapping> parameterMappings = invocation.getBoundSql().getParameterMappings();
			BoundSql newBoundSql = new BoundSql(mappedStatement.getConfiguration(), invocation.getSql(),parameterMappings, invocation.getParameter());
			//
			copyForeachAdditionlParams(invocation.getBoundSql(), newBoundSql);
			
			CacheKey cacheKey = executor.createCacheKey(mappedStatement, invocation.getParameter(), RowBounds.DEFAULT, newBoundSql);

			List<?> resultList = executor.query(mappedStatement, invocation.getParameter(), RowBounds.DEFAULT, resultHandler, cacheKey,newBoundSql);
			return resultList;
		}
	}
	
   public static void copyForeachAdditionlParams(BoundSql originBoundSql, BoundSql newBoundSql) {
		
		List<ParameterMapping> parameterMappings = originBoundSql.getParameterMappings();
		
		Object additionalParamVal;
		int itemIndex = 0;
		for (ParameterMapping parameterMapping : parameterMappings) {
			if(!parameterMapping.getProperty().startsWith(FRCH_ITEM_PREFIX)) {
				continue;
			}
			if(originBoundSql.hasAdditionalParameter(parameterMapping.getProperty())) {
				additionalParamVal = originBoundSql.getAdditionalParameter(parameterMapping.getProperty());
				newBoundSql.setAdditionalParameter(parameterMapping.getProperty(), additionalParamVal);
				newBoundSql.setAdditionalParameter(FRCH_INDEX_PREFIX + itemIndex, itemIndex);
				itemIndex++;
			}
		}
	}

	/**
	 * @param invocation
	 * @param dataMappings
	 * @return
	 */
	private void rewriteSql(InvocationVals invocation, Map<String, String[]> dataMapping) {
		String orignSql = invocation.getSql();
		PageParams pageParam = invocation.getPageParam();
		
		String currentTenantId = null;
		if(isFieldSharddingTenant) {
			currentTenantId = MybatisRuntimeContext.getCurrentTenant();
			if(currentTenantId == null)throw new JeesuiteBaseException("无法获取当前租户ID");
			if(dataMapping == null)dataMapping = new HashMap<>(1);
			dataMapping.put(TENANT_ID, new String[] {currentTenantId});
		}
		
		if(dataMapping == null && currentTenantId == null) {
			if(pageParam == null || (pageParam.getOrderBys() == null || pageParam.getOrderBys().isEmpty())) {
				return;
			}
		}
			
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
					invocation.setRewriteSql(null);
					return;
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
							return;
						}
						//左右连接加在ON 无法过滤
						newExpression = appendDataProfileCondition(table, selectBody.getWhere(), columnMapping.get(fieldName),values);
						selectBody.setWhere(newExpression);
					}
				}
			}
		}
		
		if(pageParam != null && pageParam.getOrderBys() != null && !pageParam.getOrderBys().isEmpty()) {
			 List<OrderByElement> orderByElements = new ArrayList<>(pageParam.getOrderBys().size());
				
				OrderByElement orderByElement;
				for (OrderBy orderBy : pageParam.getOrderBys()) {
					if(orderBy == null)continue;
		    		String columnName = MybatisMapperParser.property2ColumnName(invocation.getMapperNameSpace(), orderBy.getField());
		    		if(columnName == null)continue;
		    		orderByElement = new OrderByElement();
		    		orderByElement.setAsc(OrderType.ASC.name().equals(orderBy.getSortType()));
		    		orderByElement.setExpression(new Column(table, columnName));
		    		orderByElements.add(orderByElement);
				}
				
				selectBody.setOrderByElements(orderByElements);
		}
		//
		invocation.setRewriteSql(selectBody.toString());
	}
	
	
	private static Expression appendDataProfileCondition(Table table,Expression orginExpression,String columnName,String[] values){
		Expression newExpression = null;
		Column column = new Column(table, columnName);
		if (values.length == 1) {
			EqualsTo equalsTo = new EqualsTo();
			equalsTo.setLeftExpression(column);
			equalsTo.setRightExpression(new StringValue(values[0]));
			newExpression = orginExpression == null ? equalsTo : new AndExpression(equalsTo,orginExpression);
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
