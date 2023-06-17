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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import com.mendmix.common.GlobalConstants;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.model.OrderBy;
import com.mendmix.common.model.OrderBy.OrderType;
import com.mendmix.common.model.PageParams;
import com.mendmix.common.util.CachingFieldUtils;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.common.util.StringConverter;
import com.mendmix.mybatis.MybatisConfigs;
import com.mendmix.mybatis.MybatisRuntimeContext;
import com.mendmix.mybatis.core.InterceptorHandler;
import com.mendmix.mybatis.metadata.ColumnMetadata;
import com.mendmix.mybatis.metadata.MapperMetadata;
import com.mendmix.mybatis.parser.MybatisMapperParser;
import com.mendmix.mybatis.plugin.InvocationVals;
import com.mendmix.mybatis.plugin.MendmixMybatisInterceptor;
import com.mendmix.spring.InstanceFactory;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
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


	private final static Logger logger = LoggerFactory.getLogger("com.mendmix.mybatis");

	private static final String ARRAY_PARAM_START = "[";
	public static final String FRCH_PREFIX = "__frch_";
	private static final String FRCH_INDEX_PREFIX = "__frch_index_";
	private static final String FRCH_ITEM_PREFIX = "__frch_item_";
	private static final String QUERY_FUZZY_CHAR = "%";
	private static EqualsTo noPermssionCondition = new EqualsTo();
	
	private static boolean dynaDataPermEnaled = false;
	//<alias,column>
	private Map<String,String> globalDataPermColumnMappings = new HashMap<>();
	private Map<String, LinkedHashMap<String,String>> tableDataPermColumnMappings = new HashMap<>();
	private List<String[]> globalOrRelationColumns = new ArrayList<>();
	private Map<String, List<String[]>> tableOrRelationColumns = new HashMap<>();
	private List<String> softDeleteMappedStatements = new ArrayList<>();
	private String softDeleteColumnName;
	private String softDeletePropName;
	private String softDeleteFalseValue;
	
	private boolean columnSharddingTenant;
	private String tenantColumnName;
	private String tenantPropName;
	
	private String deptColumnName;
	private String deptPropName;
	private String createdByColumnName;
	private boolean orgPermFullCodeMode;
	private List<String> deptMappedStatements = new ArrayList<>();
	
	private OrganizationProvider organizationProvider;

	@Override
	public void start(MendmixMybatisInterceptor context) {
		
		noPermssionCondition = new EqualsTo();
		noPermssionCondition.setLeftExpression(new Column("1"));
		noPermssionCondition.setRightExpression(new LongValue(2));
	
		dynaDataPermEnaled = MybatisConfigs.isDataPermissionEnabled(context.getGroupName());
		columnSharddingTenant = MybatisConfigs.isColumnSharddingTenant(context.getGroupName());
		softDeleteColumnName = MybatisConfigs.getSoftDeleteColumn(context.getGroupName());
		softDeleteFalseValue = MybatisConfigs.getSoftDeletedFalseValue(context.getGroupName());
		deptColumnName = MybatisConfigs.getDeptColumnName(context.getGroupName());
		createdByColumnName = MybatisConfigs.getCreatedByColumnName(context.getGroupName());
		
		orgPermFullCodeMode = MybatisConfigs.getBoolean(context.getGroupName(),"mendmix.mybatis.dataPermission.orgPermFullCodeMode",false);
				
		final List<MapperMetadata> mappers = MybatisMapperParser.getMapperMetadatas(context.getGroupName());
		if(dynaDataPermEnaled) {
			Properties properties = ResourceUtils.getAllProperties("mendmix.mybatis.dataPermission.columns");
			properties.forEach( (k,v) -> {
				String tableName = null;
				if(k.toString().contains(ARRAY_PARAM_START)) {
					tableName = k.toString().substring(k.toString().indexOf(ARRAY_PARAM_START) + 1).replace("]", "").trim();
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
			//or group
			properties = ResourceUtils.getAllProperties("application.mybatis.dataPermission.orRelationColumns");
			properties.forEach( (k,v) -> {
				String tableName = null;
				String[] groupValues = StringUtils.split(v.toString(), ";");
				if(k.toString().contains("[")) {
					tableName = k.toString().substring(k.toString().indexOf("[") + 1).replace("]", "").trim();
				}
				String[] columns;
				for (String gv : groupValues) {
					columns = StringUtils.split(gv, ",");
					if(tableName == null) {
						globalOrRelationColumns.add(columns);
					}else {
						List<String[]> list = tableOrRelationColumns.get(tableName);
						if(list == null)tableOrRelationColumns.put(tableName, list = new ArrayList<>());
						list.add(columns);
					}
				}
			} );
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
		if(createdByColumnName != null)logBuilder.append("\n - createdByColumnName:").append(createdByColumnName);
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
		SqlRewriteStrategy rewriteStrategy = MybatisRuntimeContext.getSqlRewriteStrategy();
		if(rewriteStrategy.getRewritedTableMapping() == null && invocation.isSelectByPrimaryKey()) {
			return null;
		}
		
		if(rewriteStrategy.isIgnoreAny()) {
			return null;
		}
		//
		RewriteSqlOnceContext context;
		context = new RewriteSqlOnceContext(invocation,columnSharddingTenant, dynaDataPermEnaled);
		rewriteSql(context);
		
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
			copyAdditionlParameters(invocation.getBoundSql(), newBoundSql);
			
			CacheKey cacheKey = executor.createCacheKey(mappedStatement, invocation.getParameter(), RowBounds.DEFAULT, newBoundSql);

			List<?> resultList = executor.query(mappedStatement, invocation.getParameter(), RowBounds.DEFAULT, resultHandler, cacheKey,newBoundSql);
			return resultList;
		}
	}
	
   public static void copyAdditionlParameters(BoundSql originBoundSql, BoundSql newBoundSql) {
		List<ParameterMapping> parameterMappings = originBoundSql.getParameterMappings();
		Object additionalParamVal;
		int itemIndex = 0;
		String indexParamName;
		for (ParameterMapping parameterMapping : parameterMappings) {
			if(parameterMapping.getProperty().contains(ARRAY_PARAM_START)) {
				continue;
			}
			additionalParamVal = originBoundSql.getAdditionalParameter(parameterMapping.getProperty());
			if(parameterMapping.getProperty().startsWith(FRCH_ITEM_PREFIX)) {
				newBoundSql.setAdditionalParameter(parameterMapping.getProperty(), additionalParamVal);
				indexParamName = FRCH_INDEX_PREFIX + itemIndex;
				if(!newBoundSql.hasAdditionalParameter(indexParamName)) {					
					newBoundSql.setAdditionalParameter(indexParamName, itemIndex);
				}
				itemIndex++;
			}else if(additionalParamVal != null || parameterMapping.getProperty().startsWith(FRCH_PREFIX)){
                newBoundSql.setAdditionalParameter(parameterMapping.getProperty(), additionalParamVal);
			}
		}
	}

   /**
	 * @param invocation
	 * @return
	 */
	private void rewriteSql(RewriteSqlOnceContext context) {
		context.handleSoftDelete = !MybatisRuntimeContext.getSqlRewriteStrategy().isIgnoreSoftDelete() && (
				softDeleteMappedStatements.contains(context.invocation.getMapperNameSpace()) 
				|| softDeleteMappedStatements.contains(context.invocation.getMappedStatement().getId())
			);
		
		if(!context.withReriteRule()) {
			return;
		}
			
		SelectBody selectBody = null;
		String orignSql = context.invocation.getSql();
		try {
			Statement stmt = CCJSqlParserUtil.parse(orignSql);
			selectBody = ((Select)stmt).getSelectBody();
		} catch (JSQLParserException e) {
			logger.error("ZVOS-FRAMEWORK-TRACE-LOGGGING-->> PARSER_SQL_ERROR \n -sql:{} -\n -reason:{}",orignSql,ExceptionUtils.getRootCause(e));
			return;
		}
		
		handleSelectRewrite(context,selectBody);
		//
		context.invocation.setRewriteSql(selectBody.toString());
	}


	private void handleSelectRewrite(RewriteSqlOnceContext context,SelectBody selectBody) {
		InvocationVals invocation = context.invocation;
		if(selectBody instanceof PlainSelect) {
			PlainSelect select = (PlainSelect)selectBody;
			FromItem fromItem = select.getFromItem();
			if(fromItem instanceof Table) {
				context.parseRewriteTable(this,select);
				//
				List<RewriteTable> rewriteTables = context.rewriteTables;
				Expression newExpression;
				for (RewriteTable rewriteTable : rewriteTables) {
					if(!rewriteTable.hasRewriteColumn()) {
						continue;
					}
					newExpression = handleTableDataPermission(context,rewriteTable);
					rewriteTable.updateConditionExpression(newExpression);
				}
				//组模式org权限
				if(context.isHandleGroupOrgPermission()) {
					if(context.traceLogging) {
						logger.info(">> 开始处理全局组织权限...");
					}
					List<Expression> expressions = new ArrayList<>(rewriteTables.size());
					for (RewriteTable rewriteTable : rewriteTables) {
						if(!rewriteTable.isUsingGroupOrgPerm())continue;
						expressions.addAll(handleGroupOrgDataPermission(context,rewriteTable));
					}
					Expression mergeExpression = null;
					for (Expression expression : expressions) {	
						if(expression == null)continue;
						mergeExpression = mergeExpression == null 
								? expression 
								: new OrExpression(mergeExpression, expression);
					}
					if(mergeExpression != null) {
						Expression selectExpression = select.getWhere();
						if(selectExpression == null) {
							selectExpression = mergeExpression;
						}else {
							mergeExpression = new Parenthesis(mergeExpression);
							selectExpression = new AndExpression(selectExpression, mergeExpression);
						}
						select.setWhere(selectExpression);
					}
					if(context.traceLogging) {
						logger.info(">> 完成处理全局组织权限,重写新增条件:{}",mergeExpression);
					}
				}
				//
				if(context.handleOrderBy) {
					handleTableOrderBy(select, (Table) select.getFromItem(), invocation);
				}
			}else if(fromItem instanceof SubSelect) {
				SubSelect subSelect = (SubSelect) fromItem;
				handleSelectRewrite(context,subSelect.getSelectBody());
			}
		}else if(selectBody instanceof SetOperationList) {
			SetOperationList optList = (SetOperationList) selectBody;
			SetOperation operation = optList.getOperations().get(0);
			if(operation instanceof UnionOp) {
				
			}
			List<SelectBody> selects = optList.getSelects();
			for (SelectBody body : selects) {
				handleSelectRewrite(context,body);
			}
		}
	}
	
	private Expression handleTableDataPermission(RewriteSqlOnceContext context,RewriteTable rewriteTable) {

		String tableName = rewriteTable.getTableName();
		SqlRewriteStrategy strategy = context.strategy;
		Map<String, String[]> dataMapping = context.getDataPermValues();
		Map<String, String> rewriteColumnMapping = rewriteTable.rewriteColumnMapping;
		
		//定义的or条件列
		List<List<ConditionPair>> orConditionGroups = strategy == null ? null : strategy.getOrRelationColumns(tableName);
		if(orConditionGroups == null) {
			orConditionGroups = buildRelationColumns(tableName);
		}
		Expression permExpression = null;
		String column;
		String[] values;
		Set<String> fieldNames = rewriteColumnMapping.keySet();
		boolean withSoftDelete = false;
		boolean withPermission = false;
		boolean withAllPermission = false;
		String currentTenantId = null;
		ConditionPair condition;
		Table table = rewriteTable.getTable();
		for (String fieldName : fieldNames) {
			if(fieldName.equals(softDeletePropName)) {
				withSoftDelete = true;
				continue;
			}
			if(rewriteTable.isUsingGroupOrgPerm() && fieldName.equals(deptPropName)) {
				continue;
			}
			if(context.handleTenant && fieldName.equals(tenantPropName)) {
				if(context.mainTableHandledTenant && !rewriteTable.isAppendConditonUsingOn()) {
					continue;
				}
				column = tenantColumnName;
				currentTenantId = CurrentRuntimeContext.getTenantId();
				if(currentTenantId == null)throw new MendmixBaseException("无法获取当前租户ID[via:sqlRewrite]");
				values = new String[] {currentTenantId};
				condition = new ConditionPair(column, values);
				//
				if(!rewriteTable.isJoin()) {
					context.mainTableHandledTenant = true;
				}
			}else {
				if(dataMapping == null || !dataMapping.containsKey(fieldName))continue;
				column = rewriteColumnMapping.get(fieldName);
				values = dataMapping.get(fieldName);
				if(values != null && values.length > 0 && SpecialPermType._allValues.name().equals(values[0])) {
					withAllPermission = true;
					continue;
				}
				if(orConditionGroups == null) {
					condition = new ConditionPair(column, values);
				}else {
					boolean matched = false;
					conditionLoop:for (List<ConditionPair> conditions : orConditionGroups) {
						for (ConditionPair pair : conditions) {
							if(matched = column.equals(pair.getColumn())) {
								pair.setValues(values);
								break conditionLoop;
							}
						}
					}
					condition = matched ? null : new ConditionPair(column, values);
				}
				//
				if(!withPermission)withPermission = true;
			}
			
			if(condition != null) {
				permExpression = handleColumnDataPermCondition(rewriteTable, permExpression, condition);
			}
			if(logger.isTraceEnabled()) {
				logger.trace("ZVOS-FRAMEWORK-TRACE-LOGGGING-->> _mybatis_sqlRewrite_trace processColumn ->table:{},column:{},addtionalValues:{}",tableName,column,values);
			}
		}
		
		//
		if(orConditionGroups != null) {
			for (List<ConditionPair> conditions : orConditionGroups) {
				permExpression = handleColumnDataPermCondition(table, permExpression, conditions);
			}
		}
		
		//数据owner
		String[] ownerColumns = rewriteTable.getOwnerColumns();
		if(ownerColumns != null && ownerColumns.length > 0 && (!withAllPermission || withPermission)) {
			Expression userScopeExpression;
			boolean ignoreExistExpression = !withPermission; //无其他权限时 permExpression 只有租户条件
			for (String scopeColumn : ownerColumns) {
				if(rewriteTable.isUsingGroupOrgPerm() && !createdByColumnName.equals(scopeColumn)) {
					continue;
				}
				userScopeExpression = buildCurrentUserDataPermCondition(context,rewriteTable, scopeColumn);
				//无其他字段数据权限：租户 AND 数据owner
				if(permExpression == null || ignoreExistExpression) {
					permExpression = userScopeExpression;
					ignoreExistExpression = false;
				}else {//有其他字段数据权限：(租户 AND 数据权限) OR (租户 AND 数据owner)
					permExpression = new OrExpression(new Parenthesis(permExpression), userScopeExpression);
				}
			}
		}
		
       //原查询条件
		Expression whereExpression = rewriteTable.getOriginConditionExpression();
		if(whereExpression == null) {
			whereExpression = permExpression;
		}else if(permExpression != null) {
			if(rewriteTable.getTableStrategy() == null) {
				whereExpression = new AndExpression(new Parenthesis(whereExpression),new Parenthesis(permExpression));
			}else {				
				whereExpression = new AndExpression(new Parenthesis(permExpression),new Parenthesis(whereExpression));
			}
		}
		//软删除
		if(context.handleSoftDelete && withSoftDelete) {
			EqualsTo equalsTo = new EqualsTo();
			equalsTo.setLeftExpression(new Column(table, softDeleteColumnName));
			equalsTo.setRightExpression(new StringValue(softDeleteFalseValue));
			whereExpression = whereExpression == null ? equalsTo : new AndExpression(new Parenthesis(whereExpression), equalsTo);
		}
		
		if(context.traceLogging) {
			logger.info(">> 完成处理表：{}\n - 原条件:{}\n - 重写条件:{}",rewriteTable.getTableName()
					,rewriteTable.getOriginConditionExpression()
					,whereExpression);
		}
		return whereExpression;
	}

	
	private  Expression handleColumnDataPermCondition(RewriteTable table
			,Expression orginExpression
			,ConditionPair condition){
		Column column = new Column(table.getTable(), condition.getColumn());
		String[] values = condition.getValues();
		if(values == null || values.length == 0) {
			if(MybatisConfigs.DATA_PERM_STRICT_MODE) {
				return orginExpression;
			}
			//为空直接返回一个不成立的查询条件
			return noPermssionCondition;
		}
		Expression newExpression = orginExpression;
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
				newExpression = new AndExpression(orginExpression,expression);
			}
		} else if (values.length > 1){
			if(MybatisConfigs.DATA_PERM_ORG_USING_FULL_CODE_MODE && condition.getColumn().equals(deptColumnName)) {
				BinaryExpression itemExpression;
				BinaryExpression groupExpression = null;
				for (String value : values) {
					itemExpression = new LikeExpression();
					itemExpression.setLeftExpression(column);
					itemExpression.setRightExpression(new StringValue(value));
					if(groupExpression == null) {
						groupExpression = itemExpression;
					}else {
						groupExpression = new OrExpression(groupExpression, itemExpression);
					}
				}
				Parenthesis parenthesis = new Parenthesis(groupExpression);
				newExpression = orginExpression == null ? parenthesis : new AndExpression(orginExpression,parenthesis);
			}else {
				ExpressionList expressionList = new ExpressionList(new ArrayList<>(values.length));
				for (String value : values) {
					expressionList.getExpressions().add(new StringValue(value));
				}
				InExpression inExpression = new InExpression(column, expressionList);
				newExpression = orginExpression == null ? inExpression : new AndExpression(orginExpression,inExpression);
			}
		}
		
		return newExpression;
	}
	
	private  Expression handleColumnDataPermCondition(Table table
			,Expression orginExpression
			,List<ConditionPair> orConditions){
		
		Expression groupExpression = null;
		Column column = null;
		String[] values;
		Expression expression;
		int orCount = 0;
		for (ConditionPair condition : orConditions) {
			values = condition.getValues();
			if(values == null || values.length == 0) {
				continue;
			}
			column = new Column(table, condition.getColumn());
			if (condition.getValues().length == 1) {
				EqualsTo equalsExpr = new EqualsTo();
				equalsExpr.setLeftExpression(column);
				equalsExpr.setRightExpression(new StringValue(values[0]));
				expression = equalsExpr;
			}else {
				ExpressionList expressionList = new ExpressionList(new ArrayList<>(values.length));
				for (String value : values) {
					expressionList.getExpressions().add(new StringValue(value));
				}
				expression = new InExpression(column, expressionList);
			}
			groupExpression = groupExpression == null ? expression : new OrExpression(groupExpression, expression);
			orCount++;
		}
		//未满足任何条件，直接返回一个不成立的查询条件
		if(orCount == 0) {
			if(MybatisConfigs.DATA_PERM_STRICT_MODE) {
				return orginExpression;
			}
			return noPermssionCondition;
		}
		//
		if(orCount > 1) {
			groupExpression = new Parenthesis(groupExpression);
		}
		
		Expression newExpression = orginExpression;
		if(newExpression == null) {
			newExpression = groupExpression;
		}else if(orCount > 1) {
			newExpression = new AndExpression(newExpression,groupExpression);
		}
	
		return newExpression;
	}
	
	private List<Expression> handleGroupOrgDataPermission(RewriteSqlOnceContext context, RewriteTable rewriteTable) {
		List<Expression> expressions = new ArrayList<>(2);
		Expression expression = null;
		String[] permValues = context.getPermValues(deptPropName);
		if(permValues != null && rewriteTable.isWithDeptColumn()) {
			ConditionPair condition = new ConditionPair(deptColumnName, permValues);
			expression = handleColumnDataPermCondition(rewriteTable, null, condition);
			expressions.add(expression);
		}
		//
		if(context.currentUser != null && context.withOwnerPermission() && rewriteTable.getTableStrategy() != null) {
			String[] userScopeColumns = rewriteTable.getTableStrategy().ownerColumns();
			for (String column : userScopeColumns) {
				if(column.equals(createdByColumnName))continue;
				expression = buildCurrentUserDataPermCondition(context,rewriteTable, column);
				expressions.add(expression);
			}
		}
		
		return expressions;
	}
	

	private Expression buildCurrentUserDataPermCondition(RewriteSqlOnceContext context,RewriteTable table,String colomnName) {
		Expression expression;
		EqualsTo userEquals = new EqualsTo();
		userEquals.setLeftExpression(new Column(table.getTable(), colomnName));
		userEquals.setRightExpression(new StringValue(context.currentUser.getId()));
		if(context.currentTenantId != null && table.containsRewriteField(tenantPropName)) {
			EqualsTo tenantEquals = new EqualsTo();
			tenantEquals.setLeftExpression(new Column(table.getTable(), tenantColumnName));
			tenantEquals.setRightExpression(new StringValue(context.currentTenantId));
			expression = new Parenthesis(new AndExpression(tenantEquals, userEquals));
		}else {
			expression = userEquals;
		}
		return expression;
	}
	
	private List<List<ConditionPair>> buildRelationColumns(String table){
		List<String[]> columnRels = tableOrRelationColumns.get(table);
		if(globalOrRelationColumns.isEmpty() && columnRels == null) {
			return null;
		}
		List<List<ConditionPair>> res = new ArrayList<>(globalOrRelationColumns.size() + (columnRels == null ? 0 :columnRels.size()));
		List<ConditionPair> conditions;
		if(columnRels != null) {
			for (String[] columns : columnRels) {
	        	conditions = new ArrayList<>(columns.length);
	            for (String column : columns) {
	            	conditions.add(new ConditionPair(column, null));
				}
	            res.add(conditions);
			}
		}
		for (String[] columns : globalOrRelationColumns) {
       	conditions = new ArrayList<>(columns.length);
           for (String column : columns) {
           	conditions.add(new ConditionPair(column, null));
			}
           res.add(conditions);
		}
		return res.isEmpty() ? null : res;
	}
	
	private void handleTableOrderBy(PlainSelect selectBody, Table table, InvocationVals invocation) {
		PageParams pageParam = invocation.getPageParam();
		List<OrderByElement> orderByElements = new ArrayList<>(pageParam.getOrderBys().size());
		
		OrderByElement orderByElement;
		for (OrderBy orderBy : pageParam.getOrderBys()) {
			if (orderBy == null)
				continue;
			String columnName = MybatisMapperParser.getMapperMetadata(invocation.getMapperNameSpace())
					.property2ColumnName(orderBy.getField());
			if (columnName == null)
				columnName = orderBy.getField();
			orderByElement = new OrderByElement();
			orderByElement.setAsc(OrderType.ASC.name().equals(orderBy.getSortType()));
			orderByElement.setExpression(new Column(table, columnName));
			orderByElements.add(orderByElement);
		}
		
		selectBody.setOrderByElements(orderByElements);
	}
	
	
	public Map<String, String> getTaleAllPermColumnMapping(String tableName){
		return tableDataPermColumnMappings.get(tableName);
	}
	
	public void mergeTableColumnMapping(Map<String, String> columnMapping, String tableName) {
		if(!tableDataPermColumnMappings.containsKey(tableName)) {
			return;
		}
		LinkedHashMap<String, String> map = tableDataPermColumnMappings.get(tableName);
		if(tenantPropName != null && map.containsKey(tenantPropName)) {
			columnMapping.put(tenantPropName, map.get(tenantPropName));
		}
		if(softDeletePropName != null && map.containsKey(softDeletePropName)) {
			columnMapping.put(softDeletePropName, map.get(softDeletePropName));
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

	public String getDataPermColumnAlias(String table,String column) {
		String alias = null;
		if(tableDataPermColumnMappings.containsKey(table)) {
			alias = matchMapKeyByValue(tableDataPermColumnMappings.get(table),column);
		}
		if(alias == null) {
			alias = matchMapKeyByValue(globalDataPermColumnMappings,column);
		}
		return StringUtils.defaultString(alias, StringConverter.toCamelCase(column));
	}
	
	private String getCurtrentDepartment() {
		if(organizationProvider != null)return organizationProvider.currentDepartment();
		organizationProvider = InstanceFactory.getInstance(OrganizationProvider.class);
		return organizationProvider == null ? null : organizationProvider.currentDepartment();
	}
	
	protected void prepareSpecialPermValues(InvocationVals invocation, Map<String, String[]> dataPermValues) {
		String[] values = dataPermValues.remove(MybatisConfigs.DATA_PERM_SPEC_KEY);
		if(values == null || values.length == 0)return;
		if(SpecialPermType.owner.name().equals(values[0])) {
			MybatisRuntimeContext.getSqlRewriteStrategy().setHandleOwner(true);
			//兼容后面数据owner判断逻辑
			String virtualKey = StringUtils.join(MybatisConfigs.DATA_PERM_SPEC_KEY,GlobalConstants.COLON,SpecialPermType.owner.name());
			dataPermValues.put(virtualKey, values);
		}else if(deptMappedStatements.contains(invocation.getMapperNameSpace()) 
				|| deptMappedStatements.contains(invocation.getMappedStatement().getId())) {
			String curtrentDepartment = getCurtrentDepartment();
			if(curtrentDepartment != null) {
				Set<String> valueList = new HashSet<>(values.length);
				if(SpecialPermType.currentDept.name().equals(values[0])) {
					valueList.add(curtrentDepartment);
				}else if(SpecialPermType.currentAndSubDept.name().equals(values[0])) {
					if(orgPermFullCodeMode) {
						valueList.add(curtrentDepartment + "%");
					}else {
						List<String> subDepartmentIds = organizationProvider.subDepartments(curtrentDepartment);
					    if(subDepartmentIds != null && !subDepartmentIds.isEmpty()) {
					    	valueList.addAll(subDepartmentIds);
					    }
					}
				}else if(SpecialPermType.allDept.name().equals(values[0])) {
					valueList.add(SpecialPermType.allDept.name());
					return;
				}
				//
				if(!valueList.isEmpty()) {				
					dataPermValues.put(deptPropName, valueList.toArray(new String[0]));
				}
			}
			
		}
	}
	
	private String matchMapKeyByValue(Map<String, String> map,String expectValue) {
		if(map == null || map.isEmpty())return null;
		Optional<Entry<String, String>> optional;
		optional = map.entrySet().stream().filter(
			e -> StringUtils.equalsIgnoreCase(expectValue, e.getValue())
		).findFirst();
		if(optional.isPresent()) {
			return optional.get().getKey();
		}
		return null;
	}
	
	public boolean matchRewriteStrategy(InvocationVals invocationVal,Object result) {
		MapperMetadata meta = MybatisMapperParser.getMapperMetadata(invocationVal.getMapperNameSpace());
		if(meta == null)return true;
		try {
			//租户判断
			String tenantId = CurrentRuntimeContext.getTenantId();
			if(tenantId != null 
					&& tenantPropName != null 
					&& !MybatisRuntimeContext.getSqlRewriteStrategy().isIgnoreTenant()
					&& !matchFieldValue(meta, result, tenantPropName, tenantId)) {
				return false;
			}
			//软删除
			if(softDeletePropName != null 
					&& !MybatisRuntimeContext.getSqlRewriteStrategy().isIgnoreSoftDelete()
					&& !matchFieldValue(meta, result, softDeletePropName, softDeleteFalseValue,Boolean.FALSE.toString())) {
				return false;
			}
			//TODO 数据权限
			
		} catch (Exception e) {
			logger.error("matchRewriteStrategy_error",e);
			return true;
		}
		
		return true;
	}
	
	private boolean matchFieldValue(MapperMetadata mapperMeta,Object object,String fieldName,String...expectValues) {
		Object actualValue;
		if(mapperMeta.getPropToColumnMappings().containsKey(fieldName)) {
			actualValue = CachingFieldUtils.readField(object, fieldName);
			if(actualValue == null) {
				return true;
			}
			for (String val : expectValues) {
				if(StringUtils.equals(val, actualValue.toString())) {
					return true;
				}
			}
			return false;
		}
		return true;
	}
	
	public String getCreatedByColumnName() {
		return createdByColumnName;
	}

	public String getDeptPropName() {
		return deptPropName;
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
