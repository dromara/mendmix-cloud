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
package com.mendmix.mybatis.plugin.rewrite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.model.AuthUser;
import com.mendmix.common.model.OrderBy;
import com.mendmix.common.model.OrderBy.OrderType;
import com.mendmix.common.model.PageParams;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.common.util.StringConverter;
import com.mendmix.mybatis.MybatisConfigs;
import com.mendmix.mybatis.MybatisRuntimeContext;
import com.mendmix.mybatis.core.InterceptorHandler;
import com.mendmix.mybatis.crud.CrudMethods;
import com.mendmix.mybatis.metadata.ColumnMetadata;
import com.mendmix.mybatis.metadata.MapperMetadata;
import com.mendmix.mybatis.parser.MybatisMapperParser;
import com.mendmix.mybatis.plugin.InvocationVals;
import com.mendmix.mybatis.plugin.MendmixMybatisInterceptor;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
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

	private final static Logger logger = LoggerFactory.getLogger("com.mendmix.mybatis.plugin");

	public static final String FRCH_PREFIX = "__frch_";
	private static final String FRCH_INDEX_PREFIX = "__frch_index_";
	private static final String FRCH_ITEM_PREFIX = "__frch_item_";
	private static final String QUERY_FUZZY_CHAR = "%";
	
	private static boolean dynaDataPermEnaled = false;
	//<alias,column>
	private Map<String,String> globalDataPermColumnMappings = new HashMap<>();
	private Map<String, LinkedHashMap<String,String>> tableDataPermColumnMappings = new HashMap<>();
	
	private List<String> softDeleteMappedStatements = new ArrayList<>();
	private String softDeleteColumnName;
	private String softDeletePropName;
	private String softDeleteFalseValue;
	
	private boolean columnSharddingTenant;
	private String tenantColumnName;
	private String tenantPropName;
	
	private String orgBasePermKey;
	private String deptColumnName;
	private String deptPropName;
	private String ownerColumnName;
	private List<String> deptMappedStatements = new ArrayList<>();

	@Override
	public void start(MendmixMybatisInterceptor context) {
	
		dynaDataPermEnaled = MybatisConfigs.isDataPermissionEnabled(context.getGroupName());
		columnSharddingTenant = MybatisConfigs.isColumnSharddingTenant(context.getGroupName());
		softDeleteColumnName = MybatisConfigs.getSoftDeleteColumn(context.getGroupName());
		softDeleteFalseValue = MybatisConfigs.getSoftDeletedFalseValue(context.getGroupName());
		deptColumnName = MybatisConfigs.getDeptColumnName(context.getGroupName());
		ownerColumnName = MybatisConfigs.getOwnerColumnName(context.getGroupName());
		orgBasePermKey = MybatisConfigs.getCurrentOrgPermKey(context.getGroupName());
		
		final List<MapperMetadata> mappers = MybatisMapperParser.getMapperMetadatas(context.getGroupName());
		if(dynaDataPermEnaled) {
			Properties properties = ResourceUtils.getAllProperties("mendmix.mybatis.dataPermission.columns");
			properties.forEach( (k,v) -> {
				String tableName = null;
				if(k.toString().contains("[")) {
					tableName = k.toString().substring(k.toString().indexOf("[") + 1).replace("]", "").trim();
				}
				buildTableDataPermColumnMapping(tableName, v.toString());
				
			} );
			//全局映射配置
			for (MapperMetadata mapper : mappers) {
				Collection<String> columns = mapper.getPropToColumnMappings().values();
				Optional<Entry<String, String>> optional = globalDataPermColumnMappings.entrySet().stream().filter(e -> columns.contains(e.getValue())).findFirst();
			    if(optional.isPresent()) {
			    	if(!tableDataPermColumnMappings.containsKey(mapper.getTableName())) {
			    		tableDataPermColumnMappings.put(mapper.getTableName(), new LinkedHashMap<>());
			    	}
			    	tableDataPermColumnMappings.get(mapper.getTableName()).put(optional.get().getKey(), optional.get().getValue());
			    }
			}
		}
		//
		initColumnConfig(mappers, deptColumnName, deptMappedStatements);
		//软删除
		initColumnConfig(mappers, softDeleteColumnName, softDeleteMappedStatements);
		//字段隔离租户模式
		if (columnSharddingTenant) {
			tenantColumnName = MybatisConfigs.getTenantColumnName(context.getGroupName());

			ColumnMetadata tenantColumn;
			for (MapperMetadata mapper : mappers) {
				tenantColumn = mapper.getEntityMetadata().getColumns().stream().filter(o -> {
					return o.getColumn().equalsIgnoreCase(tenantColumnName);
				}).findFirst().orElse(null);

				if (tenantColumn == null)
					continue;
				if(tenantPropName == null)tenantPropName = tenantColumn.getProperty();
				
				if (!tableDataPermColumnMappings.containsKey(mapper.getTableName())) {
					tableDataPermColumnMappings.put(mapper.getTableName(), new LinkedHashMap<>());
				}
				tableDataPermColumnMappings.get(mapper.getTableName()).put(tenantPropName, tenantColumnName);
			}
		}
		
		StringBuilder logBuilder = new StringBuilder("MENDMIX-TRACE-LOGGGING-->> \nsqlRewrite rules:");
		if(columnSharddingTenant)logBuilder.append("\n - tenantSharddingColumn:").append(tenantColumnName);
		if(deptColumnName != null)logBuilder.append("\n - deptColumnName:").append(deptColumnName);
		if(ownerColumnName != null)logBuilder.append("\n - createdByColumnName:").append(ownerColumnName);
		if(softDeleteColumnName != null)logBuilder.append("\n - softDeleteColumn:").append(softDeleteColumnName);
		if(softDeleteFalseValue != null)logBuilder.append("\n - softDeleteFalseValue:").append(softDeleteFalseValue);
		logBuilder.append("\n - globalDataPermColumnMappings:").append(globalDataPermColumnMappings);
		logBuilder.append("\n - tableDataPermColumnMappings:").append(tableDataPermColumnMappings);
		logger.info(logBuilder.toString());
	}
	
	private void initColumnConfig(List<MapperMetadata> mappers,String column,List<String> mapperNames) {
		if(column == null)return;
		List<String> tmpTables = new ArrayList<>();
		ColumnMetadata columnMetadata;
		for (MapperMetadata mapper : mappers) {
			columnMetadata = mapper.getEntityMetadata().getColumns().stream().filter(o -> o.getColumn().equals(column)).findFirst().orElse(null);
			if(columnMetadata == null) {
				continue;
			}
			if(column.equals(softDeleteColumnName)) {
				softDeletePropName = columnMetadata.getProperty();
			}
			tmpTables.add(mapper.getTableName());
			if(!tableDataPermColumnMappings.containsKey(mapper.getTableName())) {
				tableDataPermColumnMappings.put(mapper.getTableName(), new LinkedHashMap<>());
			}
			tableDataPermColumnMappings.get(mapper.getTableName()).put(columnMetadata.getProperty(), column);
		}
		//
		for (MapperMetadata mapper : mappers) {
			if(tmpTables.contains(mapper.getTableName())) {
				mapperNames.add(mapper.getMapperClass().getName());
			}else {
				Set<String> querys = mapper.getQueryTableMappings().keySet();
				List<String> tables;
				for (String query : querys) {
					tables = mapper.getQueryTableMappings().get(query);
					for (String table : tables) {
						if(tmpTables.contains(table)) {
							mapperNames.add(query);
							break;
						}
					}
				}
			}
		}
	}

	@Override
	public Object onInterceptor(InvocationVals invocation) throws Throwable {
		if(!invocation.isSelect())return null;
		if(invocation.getMappedStatement().getId().endsWith(CrudMethods.selectByPrimaryKey.name())) {
			return null;
		}
		
		SqlRewriteStrategy rewriteStrategy = MybatisRuntimeContext.getSqlRewriteStrategy();
		if(rewriteStrategy.isIgnoreAny()) {
			return null;
		}
		//
		if(dynaDataPermEnaled && !rewriteStrategy.isIgnoreColumnPerm()) {
			Map<String, String[]> dataPermValues = MybatisRuntimeContext.getDataPermissionValues();
			invocation.setDataPermValues(dataPermValues);
		}
		//
		rewriteSql(invocation,rewriteStrategy);
		
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
	 * @param dataPermValues
	 * @return
	 */
	private void rewriteSql(InvocationVals invocation,SqlRewriteStrategy rewriteStrategy) {
		String orignSql = invocation.getSql();
		PageParams pageParam = invocation.getPageParam();
        //
		rewriteStrategy.setIgnoreTenant(!columnSharddingTenant || rewriteStrategy.isIgnoreTenant());

		Map<String, String[]> dataPermValues = invocation.getDataPermValues();
		if(dataPermValues != null && dataPermValues.containsKey(orgBasePermKey)) {
			if(deptMappedStatements.contains(invocation.getMapperNameSpace()) 
					|| deptMappedStatements.contains(invocation.getMappedStatement().getId())) {
				String departmentId = CurrentRuntimeContext.getAndValidateCurrentUser().getDeptId();
				if(StringUtils.isBlank(departmentId)) {
					throw new MendmixBaseException("当前登录用户部门ID为空");
				}
				//TODO 指定了其他部门？
				String[] values = dataPermValues.get(orgBasePermKey);
				if(values != null && values.length > 0) {
					if("leaderView".equals(values[0])) {
						dataPermValues.put(deptPropName, new String[] {departmentId + QUERY_FUZZY_CHAR});
					}else {
						dataPermValues.put(deptPropName, new String[] {departmentId});
					}
				}else {
					dataPermValues.put(deptPropName, new String[] {departmentId});
				}
			}
		}
		
		if(pageParam != null) {
			rewriteStrategy.setHandleOrderBy(pageParam.getOrderBys() != null && !pageParam.getOrderBys().isEmpty());
		}
		
		if(invocation.getDataPermValues() == null && rewriteStrategy.isIgnoreTenant() && rewriteStrategy.isIgnoreSoftDelete() && !rewriteStrategy.isHandleOrderBy()) {
			return;
		} 
		
		if(logger.isDebugEnabled()) {
			logger.debug("_mybatis_sqlRewrite_trace start -> statementId:{},rewriteStrategy:{}",JsonUtils.toJson(rewriteStrategy));
		}
			
		SelectBody selectBody = null;
		try {
			Statement stmt = CCJSqlParserUtil.parse(orignSql);
			selectBody = ((Select)stmt).getSelectBody();
		} catch (JSQLParserException e) {
			logger.error("PARSER_ERROR["+orignSql+"]",e);
			return;
		}
		
		handleSelectRewrite(selectBody, invocation,rewriteStrategy);
		//
		invocation.setRewriteSql(selectBody.toString());
	}

	
	
	private void handleSelectRewrite(SelectBody selectBody,InvocationVals invocation,SqlRewriteStrategy strategy) {
		Map<String, String[]> dataPermValues = invocation.getDataPermValues();
		if(selectBody instanceof PlainSelect) {
			PlainSelect select = (PlainSelect)selectBody;
			FromItem fromItem = select.getFromItem();
			if(fromItem instanceof Table) {
				Table table = (Table) fromItem;
				if(!strategy.isAllMatch() && !strategy.hasTableStrategy(table.getName())) {
					return;
				}
				if(logger.isTraceEnabled()) {
					logger.trace("_mybatis_sqlRewrite_trace processMainTable ->table:{}",table.getName());
				}
				//
				Expression newWhereExpression = handleTableDataPermission(select.getWhere(), table, dataPermValues,strategy,false);
				select.setWhere(newWhereExpression);
				//
				handleTableOrderBy(select, table, invocation);
				//
				List<Join> joins = select.getJoins();
				if(joins != null){
					for (Join join : joins) {
						if(join.getRightItem() instanceof Table) {
							table = (Table) join.getRightItem();
							if(logger.isTraceEnabled()) {
								logger.trace("_mybatis_sqlRewrite_trace processJoinTable ->table:{}",table.getName());
							}
							if(join.isInner()) {
								newWhereExpression = handleTableDataPermission(select.getWhere(), table, dataPermValues, strategy,true);
								select.setWhere(newWhereExpression);
							}else {
								newWhereExpression = handleTableDataPermission(join.getOnExpression(), table, dataPermValues, strategy,true);
								join.setOnExpression(newWhereExpression);
							}
						}else {
							//TODO 
						}
					}
				}
			}else if(fromItem instanceof SubSelect) {
				SubSelect subSelect = (SubSelect) fromItem;
				handleSelectRewrite(subSelect.getSelectBody() ,invocation,strategy);
			}
		}else if(selectBody instanceof SetOperationList) {
			SetOperationList optList = (SetOperationList) selectBody;
			SetOperation operation = optList.getOperations().get(0);
			if(operation instanceof UnionOp) {
				
			}
			List<SelectBody> selects = optList.getSelects();
			for (SelectBody body : selects) {
				handleSelectRewrite(body,invocation,strategy);
			}
		}
	}
	
	private Expression handleTableDataPermission(Expression whereExpression,Table table,Map<String, String[]> dataMapping,SqlRewriteStrategy strategy,boolean isJoin) {
		
		Map<String, String> columnMapping = null;
		boolean handleDataPerm = !isJoin || strategy.isHandleJoin() || strategy.hasTableStrategy(table.getName());
		if(handleDataPerm) {
			if(!strategy.isAllMatch()) {
				String[] columns = strategy.getTableStrategy(table.getName()).columns();
				if(columns.length > 0) {
					columnMapping = new HashMap<>(columns.length);
					for (String column : columns) {
						columnMapping.put(getDataPermColumnAlias(table.getName(), column),column);
					}
					mergeTableColumnMapping(columnMapping, table.getName(), tenantPropName,softDeletePropName,deptPropName);
				}
			}
			if(columnMapping == null) {
				columnMapping = tableDataPermColumnMappings.get(table.getName());
			}
		}else {
			columnMapping = new HashMap<>(2);
			mergeTableColumnMapping(columnMapping, table.getName(), tenantPropName,softDeletePropName);
		}
		
		if(columnMapping == null || columnMapping.isEmpty()) {
			return whereExpression;
		}
		
		Expression permExpression = null;
		String column;
		String[] values;
		Set<String> fieldNames = columnMapping.keySet();
		boolean withSoftDelete = false;
		boolean withPermission = false;
		for (String fieldName : fieldNames) {
			if(fieldName.equals(softDeletePropName)) {
				withSoftDelete = true;
				continue;
			}
			if(!strategy.isIgnoreTenant() && fieldName.equals(tenantPropName)) {
				column = tenantColumnName;
				String currentTenantId = CurrentRuntimeContext.getTenantId();
				if(currentTenantId == null)throw new MendmixBaseException("无法获取当前租户ID");
				values = new String[] {currentTenantId};
			}else {
				if(dataMapping == null || !dataMapping.containsKey(fieldName))continue;
				column = columnMapping.get(fieldName);
				values = dataMapping.get(fieldName);
				//
				if(!withPermission)withPermission = true;
			}
			//如果某个匹配字段为空构造一个特殊的不等于条件
			if(values == null || values.length == 0) {
				EqualsTo equalsTo = new EqualsTo();
				equalsTo.setLeftExpression(new Column(table, column));
				equalsTo.setRightExpression(new StringValue("__DATA_PERMISSION_NULL__"));
				permExpression = equalsTo;
				//break; 后面条件不处理 ，mybatis占位符可以出错
			}else {
				permExpression = handleColumnDataPermCondition(table, permExpression, column,values);
			}
			
			if(logger.isTraceEnabled()) {
				logger.trace("_mybatis_sqlRewrite_trace processColumn ->table:{},column:{},addtionalValues:{}",table.getName(),column,values);
			}
		}
		
		//当前创建人
		if(withPermission && ownerColumnName != null && strategy.handleOwner(table.getName())) {
			AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
			if(currentUser != null) {
				EqualsTo equalsTo = new EqualsTo();
				equalsTo.setLeftExpression(new Column(table, ownerColumnName));
				equalsTo.setRightExpression(new StringValue(currentUser.getId()));
				//
				permExpression = permExpression == null ? equalsTo : new OrExpression(new Parenthesis(permExpression), equalsTo);
			}
		}
		//原查询条件
		if(whereExpression == null) {
			whereExpression = permExpression;
		}else if(permExpression != null) {
			whereExpression = new AndExpression(new Parenthesis(permExpression), new Parenthesis(whereExpression));
		}
		//软删除
		if(withSoftDelete && !strategy.isIgnoreSoftDelete()) {
			EqualsTo equalsTo = new EqualsTo();
			equalsTo.setLeftExpression(new Column(table, softDeleteColumnName));
			equalsTo.setRightExpression(new StringValue(softDeleteFalseValue));
			whereExpression = whereExpression == null ? equalsTo : new AndExpression(new Parenthesis(whereExpression), equalsTo);
		}
		
		return whereExpression;
	}

	
	private  Expression handleColumnDataPermCondition(Table table,Expression orginExpression,String columnName,String[] values){
		Expression newExpression = orginExpression;
		Column column = new Column(table, columnName);
		if (values.length == 1) {
			BinaryExpression expression;
			if(values[0].endsWith(QUERY_FUZZY_CHAR)) {
				expression = new LikeExpression();
				expression.setLeftExpression(column);
				expression.setRightExpression(new StringValue(values[0]));
			}else {
				expression = new EqualsTo();
				expression.setLeftExpression(column);
				expression.setRightExpression(new StringValue(values[0]));
			}

			if(orginExpression == null) {
				newExpression = expression;
			}else {
				if(columnName.equalsIgnoreCase(softDeleteColumnName)) {
					newExpression = new AndExpression(orginExpression,expression);
				}else {
					newExpression = new AndExpression(expression,orginExpression);
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
			if (orderBy == null)
				continue;
			MapperMetadata mapperMeta = MybatisMapperParser.getMapperMetadata(invocation.getMapperNameSpace());
			String columnName = mapperMeta.getEntityMetadata().getProp2ColumnMappings().get(orderBy.getField());
			if (columnName == null)
				columnName = orderBy.getField();
			orderByElement = new OrderByElement();
			orderByElement.setAsc(OrderType.ASC.name().equals(orderBy.getSortType()));
			orderByElement.setExpression(new Column(table, columnName));
			orderByElements.add(orderByElement);
			if (logger.isTraceEnabled()) {
				logger.trace("_mybatis_sqlRewrite_trace processOrderBy ->table:{},columnName:{}", table.getName(),
						columnName);
			}
		}
		
		selectBody.setOrderByElements(orderByElements);
	}
	
	
	private void mergeTableColumnMapping(Map<String, String> columnMapping, String tableName, String...propNames) {
		if(!tableDataPermColumnMappings.containsKey(tableName)) {
			return;
		}
		LinkedHashMap<String, String> map = tableDataPermColumnMappings.get(tableName);
		for (String propName : propNames) {
			if(map.containsKey(propName)) {
				columnMapping.put(propName, map.get(propName));
			}
		}
	}

	private void buildTableDataPermColumnMapping(String tableName,String columnValue) {
		if(tableName != null) {
			tableDataPermColumnMappings.put(tableName, new LinkedHashMap<>());
		}
		String[] columns = columnValue.split(",|;");
		String[] tmpArr;
		for (String column : columns) {
			tmpArr = column.split(":");
			String aliasName = tmpArr.length == 2 ? tmpArr[1] : StringConverter.toCamelCase(column);
			if(tableName != null) {
				tableDataPermColumnMappings.get(tableName).put(aliasName,tmpArr[0]);
			}else {
				globalDataPermColumnMappings.put(aliasName,tmpArr[0]);
				if(deptPropName == null && tmpArr[0].equals(deptColumnName)) {
					deptPropName = aliasName;
				}
			}
		}
	}

	private String getDataPermColumnAlias(String table,String column) {
		String alias = null;
		if(tableDataPermColumnMappings.containsKey(table)) {
			alias = tableDataPermColumnMappings.get(table).get(column);
		}
		if(alias == null) {
			alias = globalDataPermColumnMappings.get(column);
		}
		return StringUtils.defaultString(alias, StringConverter.toCamelCase(column));
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
