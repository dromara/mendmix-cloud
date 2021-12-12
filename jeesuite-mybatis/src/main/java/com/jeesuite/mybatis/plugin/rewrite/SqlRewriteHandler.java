package com.jeesuite.mybatis.plugin.rewrite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.model.OrderBy;
import com.jeesuite.common.model.OrderBy.OrderType;
import com.jeesuite.common.model.PageParams;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.mybatis.MybatisConfigs;
import com.jeesuite.mybatis.MybatisRuntimeContext;
import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.crud.CrudMethods;
import com.jeesuite.mybatis.metadata.ColumnMetadata;
import com.jeesuite.mybatis.metadata.MapperMetadata;
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
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SetOperation;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.UnionOp;

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

	public static final String FRCH_PREFIX = "__frch_";
	private static final String FRCH_INDEX_PREFIX = "__frch_index_";
	private static final String FRCH_ITEM_PREFIX = "__frch_item_";
	
	private Map<String, LinkedHashMap<String,String>> dataProfileMappings = new HashMap<>();
	
	private List<String> softDeleteMappedStatements = new ArrayList<>();
	private String softDeleteColumn;
	private String softDeleteFalseValue;
	
	private boolean isFieldSharddingTenant;
	private String tenantColumnName;
	private String tenantPropName;

	@Override
	public void start(JeesuiteMybatisInterceptor context) {
	
		isFieldSharddingTenant = MybatisConfigs.isFieldSharddingTenant(context.getGroupName());
		softDeleteColumn = MybatisConfigs.getSoftDeleteColumn(context.getGroupName());
		softDeleteFalseValue = MybatisConfigs.getSoftDeletedFalseValue(context.getGroupName());
		
		Properties properties = ResourceUtils.getAllProperties("jeesuite.mybatis.dataPermission.mappings");
		properties.forEach( (k,v) -> {
			String tableName = k.toString().substring(k.toString().indexOf("[") + 1).replace("]", "").trim();
			buildTableDataPermissionMapping(tableName, v.toString());
		} );
		
		final List<MapperMetadata> mappers = MybatisMapperParser.getMapperMetadatas(context.getGroupName());
		//软删除
		if(softDeleteColumn != null) {
			List<String> softDeleteTables = new ArrayList<>();
			for (MapperMetadata mapper : mappers) {
				if(!mapper.getEntityMetadata().getColumns().stream().anyMatch(o -> o.getColumn().equals(softDeleteColumn))) {
					continue;
				}
				softDeleteTables.add(mapper.getTableName());
				if(!dataProfileMappings.containsKey(mapper.getTableName())) {
					dataProfileMappings.put(mapper.getTableName(), new LinkedHashMap<>());
				}
				dataProfileMappings.get(mapper.getTableName()).put(softDeleteColumn, softDeleteColumn);
			}
			//
			for (MapperMetadata mapper : mappers) {
				if(softDeleteTables.contains(mapper.getTableName().toLowerCase())) {
					softDeleteMappedStatements.add(mapper.getMapperClass().getName());
				}else {
					Set<String> querys = mapper.getQueryTableMappings().keySet();
					List<String> tables;
					for (String query : querys) {
						tables = mapper.getQueryTableMappings().get(query);
						for (String table : tables) {
							if(softDeleteTables.contains(table)) {
								softDeleteMappedStatements.add(query);
								break;
							}
						}
					}
				}
			}
		}
		//字段隔离租户模式
		if (isFieldSharddingTenant) {
			String tenantField = MybatisConfigs.getTenantSharddingField(context.getGroupName());

			ColumnMetadata tenantColumn;
			for (MapperMetadata mapper : mappers) {
				tenantColumn = mapper.getEntityMetadata().getColumns().stream().filter(o -> {
					return o.getColumn().equals(tenantField) || o.getProperty().equals(tenantField);
				}).findFirst().orElse(null);

				if (tenantColumn == null)
					continue;

				if(tenantColumnName == null)tenantColumnName = tenantColumn.getColumn();
				if(tenantPropName == null)tenantPropName = tenantColumn.getProperty();
				
				if (!dataProfileMappings.containsKey(mapper.getTableName())) {
					dataProfileMappings.put(mapper.getTableName(), new LinkedHashMap<>());
				}
				dataProfileMappings.get(mapper.getTableName()).put(tenantPropName, tenantColumnName);
			}
		}
		
		logger.info("dataProfileMappings >> {}",dataProfileMappings);
	}

	@Override
	public Object onInterceptor(InvocationVals invocation) throws Throwable {
		if(!invocation.isSelect())return null;
		if(invocation.getMappedStatement().getId().endsWith(CrudMethods.selectByPrimaryKey.name())) {
			return null;
		}
		Map<String, String[]> dataMappings = MybatisRuntimeContext.getDataProfileMappings();
		//
		rewriteSql(invocation, dataMappings);
		
		if(invocation.getPageParam() != null)return null;
        //不查数据库直接返回
		if(invocation.getSql() == null) {
			List<Object> list = new ArrayList<>(1);
			//
			MapperMetadata entityInfo = MybatisMapperParser.getMapperMetadata(invocation.getMapperNameSpace());
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
			if(!parameterMapping.getProperty().startsWith(FRCH_PREFIX)) {
				continue;
			}
			if(originBoundSql.hasAdditionalParameter(parameterMapping.getProperty())) {
				additionalParamVal = originBoundSql.getAdditionalParameter(parameterMapping.getProperty());
				newBoundSql.setAdditionalParameter(parameterMapping.getProperty(), additionalParamVal);
				if(parameterMapping.getProperty().startsWith(FRCH_ITEM_PREFIX)) {
					newBoundSql.setAdditionalParameter(FRCH_INDEX_PREFIX + itemIndex, itemIndex);
					itemIndex++;
				}
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
		
		boolean sharddingTenant = false;
		if(isFieldSharddingTenant && !CurrentRuntimeContext.getIgnoreTenant()) {
			sharddingTenant = true;
		}
		
		boolean softDelete = softDeleteMappedStatements.contains(invocation.getMapperNameSpace()) 
				|| softDeleteMappedStatements.contains(invocation.getMappedStatement().getId());
		if(softDelete) {
			if(dataMapping == null)dataMapping = new HashMap<>(1);
			dataMapping.put(softDeleteColumn, new String[] {softDeleteFalseValue});
		}
		if(!softDelete && dataMapping == null && !sharddingTenant) {
			if(pageParam == null || (pageParam.getOrderBys() == null || pageParam.getOrderBys().isEmpty())) {
				return;
			}
		}
			
		SelectBody selectBody = null;
		try {
			Statement stmt = CCJSqlParserUtil.parse(orignSql);
			selectBody = ((Select)stmt).getSelectBody();
		} catch (JSQLParserException e) {
			logger.error("PARSER_ERROR["+orignSql+"]",e);
			throw new RuntimeException("sql解析错误");
		}
		
		handleSelectRewrite(selectBody, invocation, dataMapping, sharddingTenant);
		//
		invocation.setRewriteSql(selectBody.toString());
	}

	
	
	private boolean handleSelectRewrite(SelectBody selectBody,InvocationVals invocation,Map<String, String[]> dataMapping,boolean sharddingTenant) {
		
		if(selectBody instanceof PlainSelect) {
			PlainSelect select = (PlainSelect)selectBody;
			FromItem fromItem = select.getFromItem();
			if(fromItem instanceof Table) {
				Table table = (Table) fromItem;
				//
				handleTableDataPermission(select, table, dataMapping, sharddingTenant);
				//
				handleTableOrderBy(select, table, invocation);
				//
				List<Join> joins = select.getJoins();
				if(joins != null){
					for (Join join : joins) {
						table = (Table) join.getRightItem();
						handleTableDataPermission(select, table, dataMapping, sharddingTenant);
					}
				}
			}else if(fromItem instanceof SubSelect) {
				SubSelect subSelect = (SubSelect) fromItem;
				handleSelectRewrite(subSelect.getSelectBody() ,invocation, dataMapping, sharddingTenant);
			}
		}else if(selectBody instanceof SetOperationList) {
			SetOperationList optList = (SetOperationList) selectBody;
			SetOperation operation = optList.getOperations().get(0);
			if(operation instanceof UnionOp) {
				
			}
			List<SelectBody> selects = optList.getSelects();
			for (SelectBody body : selects) {
				handleSelectRewrite(body,invocation, dataMapping, sharddingTenant);
			}
		}
		
		return true;
	}
	
	private boolean handleTableDataPermission(PlainSelect selectBody,Table table,Map<String, String[]> dataMapping,boolean sharddingTenant) {
		if(!dataProfileMappings.containsKey(table.getName())) {
			return true;
		}
		Set<String> fieldNames;
		Expression newExpression = null;
		String column;
		String[] values;
		Map<String, String> columnMapping = dataProfileMappings.get(table.getName());
		fieldNames = columnMapping.keySet();
		for (String fieldName : fieldNames) {
			if(sharddingTenant && fieldName.equals(tenantPropName)) {
				column = tenantColumnName;
				String currentTenantId = CurrentRuntimeContext.getTenantId();
				if(currentTenantId == null)throw new JeesuiteBaseException("无法获取当前租户ID");
				values = new String[] {currentTenantId};
			}else {
				if(!dataMapping.containsKey(fieldName))continue;
				column = columnMapping.get(fieldName);
				values = dataMapping.get(fieldName);
			}
			//如果某个匹配字段为空直接返回null，不在查询数据库
			if(values == null || values.length == 0) {
				return false;
			}
			newExpression = handleColumnDataPermCondition(table, selectBody.getWhere(), column,values);
			selectBody.setWhere(newExpression);
		}
		
		return true;
	}
	
	private  Expression handleColumnDataPermCondition(Table table,Expression orginExpression,String columnName,String[] values){
		Expression newExpression = null;
		Column column = new Column(table, columnName);
		if (values.length == 1) {
			EqualsTo equalsTo = new EqualsTo();
			equalsTo.setLeftExpression(column);
			equalsTo.setRightExpression(new StringValue(values[0]));
			if(orginExpression == null) {
				newExpression = equalsTo;
			}else {
				if(columnName.equalsIgnoreCase(softDeleteColumn)) {
					newExpression = new AndExpression(orginExpression,equalsTo);
				}else {
					newExpression = new AndExpression(equalsTo,orginExpression);
				}
			}
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
	
	private void handleTableOrderBy(PlainSelect selectBody, Table table, InvocationVals invocation) {
		PageParams pageParam = invocation.getPageParam();
		if(pageParam == null || pageParam.getOrderBys() == null || pageParam.getOrderBys().isEmpty()) {
			 return;
		}
		List<OrderByElement> orderByElements = new ArrayList<>(pageParam.getOrderBys().size());
		
		OrderByElement orderByElement;
		for (OrderBy orderBy : pageParam.getOrderBys()) {
			if(orderBy == null)continue;
			MapperMetadata mapperMeta = MybatisMapperParser.getMapperMetadata(invocation.getMapperNameSpace());
    		String columnName = mapperMeta.getEntityMetadata().getProp2ColumnMappings().get(orderBy.getField());
    		if(columnName == null)columnName = orderBy.getField();
    		orderByElement = new OrderByElement();
    		orderByElement.setAsc(OrderType.ASC.name().equals(orderBy.getSortType()));
    		orderByElement.setExpression(new Column(table, columnName));
    		orderByElements.add(orderByElement);
		}
		
		selectBody.setOrderByElements(orderByElements);
	}
	
	private void buildTableDataPermissionMapping(String tableName,String ruleString) {
		dataProfileMappings.put(tableName, new LinkedHashMap<>());
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
