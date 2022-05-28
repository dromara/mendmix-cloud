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

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.JeesuiteBaseException;
import com.mendmix.common.constants.MatchPolicy;
import com.mendmix.common.model.AuthUser;
import com.mendmix.common.model.OrderBy;
import com.mendmix.common.model.OrderBy.OrderType;
import com.mendmix.common.model.PageParams;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.mybatis.MybatisConfigs;
import com.mendmix.mybatis.MybatisRuntimeContext;
import com.mendmix.mybatis.core.InterceptorHandler;
import com.mendmix.mybatis.crud.CrudMethods;
import com.mendmix.mybatis.kit.MybatisSqlUtils;
import com.mendmix.mybatis.metadata.ColumnMetadata;
import com.mendmix.mybatis.metadata.MapperMetadata;
import com.mendmix.mybatis.parser.MybatisMapperParser;
import com.mendmix.mybatis.plugin.InvocationVals;
import com.mendmix.mybatis.plugin.JeesuiteMybatisInterceptor;

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
	
	private Map<String, LinkedHashMap<String,String>> dataPermMappings = new HashMap<>();
	
	private static boolean dynaDataPermEnabled = false;
	
	private List<String> softDeleteMappedStatements = new ArrayList<>();
	private String softDeleteColumnName;
	private String softDeletePropName;
	private String softDeleteFalseValue;
	
	private boolean isFieldSharddingTenant;
	private String tenantColumnName;
	private String tenantPropName;
	
	private String orgBasePermKey;
	private String deptColumnName;
	private String deptPropName;
	private String ownerColumnName;
	private List<String> deptMappedStatements = new ArrayList<>();

	@Override
	public void start(JeesuiteMybatisInterceptor context) {
	
		isFieldSharddingTenant = MybatisConfigs.isFieldSharddingTenant(context.getGroupName());
		softDeleteColumnName = MybatisConfigs.getSoftDeleteColumn(context.getGroupName());
		softDeleteFalseValue = MybatisConfigs.getSoftDeletedFalseValue(context.getGroupName());
		deptColumnName = MybatisConfigs.getDeptColumnName(context.getGroupName());
		ownerColumnName = MybatisConfigs.getOwnerColumnName(context.getGroupName());
		orgBasePermKey = MybatisConfigs.getCurrentOrgPermKey(context.getGroupName());
		
		Properties properties = ResourceUtils.getAllProperties("mendmix.mybatis.permission.table-column-mappings");
		properties.forEach( (k,v) -> {
			String tableName = k.toString().substring(k.toString().indexOf("[") + 1).replace("]", "").trim();
			buildTableDataPermissionMapping(tableName, v.toString());
		} );
		
		dynaDataPermEnabled = !dataPermMappings.isEmpty();
		
		final List<MapperMetadata> mappers = MybatisMapperParser.getMapperMetadatas(context.getGroupName());
		//
		initColumnConfig(mappers, deptColumnName, deptMappedStatements);
		//软删除
		initColumnConfig(mappers, softDeleteColumnName, softDeleteMappedStatements);
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
				
				if (!dataPermMappings.containsKey(mapper.getTableName())) {
					dataPermMappings.put(mapper.getTableName(), new LinkedHashMap<>());
				}
				dataPermMappings.get(mapper.getTableName()).put(tenantPropName, tenantColumnName);
			}
		}
		
		logger.info("dataProfileMappings >> {}",dataPermMappings);
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
			if(!dataPermMappings.containsKey(mapper.getTableName())) {
				dataPermMappings.put(mapper.getTableName(), new LinkedHashMap<>());
			}
			dataPermMappings.get(mapper.getTableName()).put(columnMetadata.getProperty(), column);
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
		Map<String, String[]> dataMappings = null;
		if(dynaDataPermEnabled) {
			dataMappings = MybatisRuntimeContext.getDataProfileMappings();
		}
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
			dataMapping.put(softDeletePropName, new String[] {softDeleteFalseValue});
		}

		if(deptPropName != null && dataMapping != null && dataMapping.containsKey(orgBasePermKey)) {
			if(deptMappedStatements.contains(invocation.getMapperNameSpace()) 
					|| deptMappedStatements.contains(invocation.getMappedStatement().getId())) {
				String departmentId = CurrentRuntimeContext.getAndValidateCurrentUser().getDeptId();
				if(StringUtils.isBlank(departmentId)) {
					throw new JeesuiteBaseException("当前登录用户部门ID为空");
				}
				String[] values = dataMapping.get(orgBasePermKey);
				if(values != null && values.length > 0) {
					if(MatchPolicy.exact.name().equals(values[0])) {
						dataMapping.put(deptPropName, new String[] {departmentId + QUERY_FUZZY_CHAR});
					}else {
						dataMapping.put(deptPropName, new String[] {departmentId});
					}
				}else {
					dataMapping.put(deptPropName, new String[] {departmentId});
				}
			}
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
				Expression newWhere = handleTableDataPermission(select.getWhere(), table, dataMapping, sharddingTenant);
				select.setWhere(newWhere);
				//
				handleTableOrderBy(select, table, invocation);
				//
				List<Join> joins = select.getJoins();
				if(joins != null){
					for (Join join : joins) {
						table = (Table) join.getRightItem();
						newWhere = handleTableDataPermission(join.getOnExpression(), table, dataMapping, sharddingTenant);
						join.setOnExpression(newWhere);
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
	
	private Expression handleTableDataPermission(Expression originWhere,Table table,Map<String, String[]> dataMapping,boolean sharddingTenant) {
		if(!dataPermMappings.containsKey(table.getName())) {
			return originWhere;
		}
		Set<String> fieldNames;
		Expression newExpression = originWhere;
		String column;
		String[] values;
		Map<String, String> columnMapping = dataPermMappings.get(table.getName());
		fieldNames = columnMapping.keySet();
		boolean withSoftDelete = false;
		boolean withPermiCondition = false;
		for (String fieldName : fieldNames) {
			if(fieldName.equals(softDeletePropName)) {
				withSoftDelete = true;
				continue;
			}
			if(sharddingTenant && fieldName.equals(tenantPropName)) {
				column = tenantColumnName;
				String currentTenantId = CurrentRuntimeContext.getTenantId();
				if(currentTenantId == null)throw new JeesuiteBaseException("无法获取当前租户ID");
				values = new String[] {currentTenantId};
			}else {
				if(dataMapping == null || !dataMapping.containsKey(fieldName))continue;
				column = columnMapping.get(fieldName);
				values = dataMapping.get(fieldName);
				//
				if(!withPermiCondition)withPermiCondition = true;
			}
			//如果某个匹配字段为空直接返回null，不在查询数据库
			if(values == null || values.length == 0) {
				EqualsTo equalsTo = new EqualsTo();
				equalsTo.setLeftExpression(new Column(table, column));
				equalsTo.setRightExpression(new StringValue("_PERMISSION_NOT_MATCH_"));
				return equalsTo;
			}
			newExpression = handleColumnDataPermCondition(table, newExpression, column,values);
		}
		
		//当前创建人
		if(withPermiCondition && ownerColumnName != null) {
			AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
			if (currentUser != null) {
				EqualsTo equalsTo = new EqualsTo();
				equalsTo.setLeftExpression(new Column(table, ownerColumnName));
				// TODO 需要按ID匹配否则出现同名
				equalsTo.setRightExpression(new StringValue(currentUser.getName()));
				//
				newExpression = newExpression == null ? equalsTo : new OrExpression(new Parenthesis(newExpression), equalsTo);
			}
		}
		//软删除
		if (withSoftDelete) {
			EqualsTo equalsTo = new EqualsTo();
			equalsTo.setLeftExpression(new Column(table, softDeleteColumnName));
			equalsTo.setRightExpression(new StringValue(softDeleteFalseValue));
			newExpression = newExpression == null ? equalsTo : new AndExpression(new Parenthesis(newExpression), equalsTo);
		}
		
		return newExpression;
	}
	
	private  Expression handleColumnDataPermCondition(Table table,Expression orginExpression,String columnName,String[] values){
		Expression newExpression = null;
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
		dataPermMappings.put(tableName, new LinkedHashMap<>());
		String[] rules = ruleString.split(",|;");
		String[] tmpArr;
		for (String rule : rules) {
			tmpArr = rule.split(":");
			String columnName = tmpArr[0];
			String propName = tmpArr.length == 2 ? tmpArr[1] : MybatisSqlUtils.underscoreToCamelCase(columnName);
			dataPermMappings.get(tableName).put(propName, columnName);
			if(deptPropName == null && columnName.equals(deptColumnName)) {
				deptPropName = propName;
			}
		}
	}
	
	

	public static boolean isDynaDataPermEnabled() {
		return dynaDataPermEnabled;
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
