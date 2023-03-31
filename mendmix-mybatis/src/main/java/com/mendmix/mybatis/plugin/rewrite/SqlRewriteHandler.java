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
import com.mendmix.common.util.CachingFieldUtils;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.common.util.StringConverter;
import com.mendmix.mybatis.MybatisConfigs;
import com.mendmix.mybatis.MybatisRuntimeContext;
import com.mendmix.mybatis.core.InterceptorHandler;
import com.mendmix.mybatis.metadata.ColumnMetadata;
import com.mendmix.mybatis.metadata.MapperMetadata;
import com.mendmix.mybatis.metadata.MetadataHelper;
import com.mendmix.mybatis.parser.MybatisMapperParser;
import com.mendmix.mybatis.plugin.InvocationVals;
import com.mendmix.mybatis.plugin.MendmixMybatisInterceptor;
import com.mendmix.spring.InstanceFactory;

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


	private final static Logger logger = LoggerFactory.getLogger("com.mendmix.mybatis");

	private static final String ARRAY_PARAM_START = "[";
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
	
	private String specialPermKey; //  组织架构、数据owner
	private String deptColumnName;
	private String deptPropName;
	private String createdByColumnName;
	private boolean defaultHandleOwner;
	private boolean orgPermFullCodeMode;
	private List<String> deptMappedStatements = new ArrayList<>();
	
	private OrganizationProvider organizationProvider;

	@Override
	public void start(MendmixMybatisInterceptor context) {
	
		dynaDataPermEnaled = MybatisConfigs.isDataPermissionEnabled(context.getGroupName());
		columnSharddingTenant = MybatisConfigs.isColumnSharddingTenant(context.getGroupName());
		softDeleteColumnName = MybatisConfigs.getSoftDeleteColumn(context.getGroupName());
		softDeleteFalseValue = MybatisConfigs.getSoftDeletedFalseValue(context.getGroupName());
		deptColumnName = MybatisConfigs.getDeptColumnName(context.getGroupName());
		createdByColumnName = MybatisConfigs.getCreatedByColumnName(context.getGroupName());
		specialPermKey = MybatisConfigs.getProperty(context.getGroupName(), "mendmix.mybatis.dataPermission.specialPermKey", "internalSpecPermKey");
		
		defaultHandleOwner = MybatisConfigs.getBoolean(context.getGroupName(),"mendmix.mybatis.dataPermission.defaultHandleOwner", createdByColumnName != null);
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
	 * @param dataPermValues
	 * @return
	 */
	private void rewriteSql(InvocationVals invocation,SqlRewriteStrategy rewriteStrategy) {
		String orignSql = invocation.getSql();
		PageParams pageParam = invocation.getPageParam();
        //
		rewriteStrategy.setIgnoreTenant(!columnSharddingTenant || rewriteStrategy.isIgnoreTenant());

		Map<String, String[]> dataPermValues = invocation.getDataPermValues();
		//特殊的数据权限：组织机构、数据owner等
		if(dataPermValues != null && dataPermValues.containsKey(specialPermKey)) {
			String[] values = dataPermValues.remove(specialPermKey);
			if(SpecialPermType.owner.name().equals(values[0])) {
				MybatisRuntimeContext.getSqlRewriteStrategy().setHandleOwner(true);
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
		
		if(pageParam != null) {
			rewriteStrategy.setHandleOrderBy(pageParam.getOrderBys() != null && !pageParam.getOrderBys().isEmpty());
		}
		
		if(invocation.getDataPermValues() == null 
				&& rewriteStrategy.getRewritedTableMapping() == null 
				&& rewriteStrategy.isIgnoreTenant() 
				&& rewriteStrategy.isIgnoreSoftDelete() 
				&& !rewriteStrategy.isHandleOrderBy()) {
			return;
		} 
		
		if(logger.isDebugEnabled()) {
			logger.debug("_mybatis_sqlRewrite_trace for:[{}] start -> rewriteStrategy:\n{}",JsonUtils.toJson(rewriteStrategy));
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
		Map<String, String> rewritedTableMapping = strategy.getRewritedTableMapping();
		Map<String, String[]> dataPermValues = invocation.getDataPermValues();
		if(selectBody instanceof PlainSelect) {
			PlainSelect select = (PlainSelect)selectBody;
			FromItem fromItem = select.getFromItem();
			if(fromItem instanceof Table) {
				Table table = (Table) fromItem;
                //分表
				if(rewritedTableMapping != null && rewritedTableMapping.containsKey(table.getName())) {
					table.setName(rewritedTableMapping.get(table.getName()));
					select.setFromItem(table);
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
							if(rewritedTableMapping != null && rewritedTableMapping.containsKey(table.getName())) {
								table.setName(rewritedTableMapping.get(table.getName()));
								join.setRightItem(table);
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
				//TODO 
			}
			List<SelectBody> selects = optList.getSelects();
			for (SelectBody body : selects) {
				handleSelectRewrite(body,invocation,strategy);
			}
		}
	}
	
	private Expression handleTableDataPermission(Expression whereExpression,Table table,Map<String, String[]> dataMapping,SqlRewriteStrategy strategy,boolean isJoin) {
		
		Map<String, String> columnMapping = null;
		String[] userOwnerColumns = null; //当前用户关联的列
		boolean handleDataPerm =  (!isJoin || strategy.isHandleJoin()) && strategy.hasTableStrategy(table.getName());	
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
		
		//定义的or条件列
		List<List<ConditionPair>> orConditionGroups = strategy == null ? null : strategy.getOrRelationColumns(table.getName());
		
		Expression permExpression = null;
		String column;
		String[] values;
		Set<String> fieldNames = columnMapping.keySet();
		boolean withSoftDelete = false;
		boolean withPermission = false;
		String currentTenantId = null;
		ConditionPair condition;
		for (String fieldName : fieldNames) {
			if(fieldName.equals(softDeletePropName)) {
				withSoftDelete = true;
				continue;
			}
			if(!strategy.isIgnoreTenant() && fieldName.equals(tenantPropName)) {
				column = tenantColumnName;
				currentTenantId = CurrentRuntimeContext.getTenantId();
				if(currentTenantId == null)throw new MendmixBaseException("无法获取当前租户ID");
				values = new String[] {currentTenantId};
				condition = new ConditionPair(column, values);
			}else {
				if(dataMapping == null || !dataMapping.containsKey(fieldName))continue;
				column = columnMapping.get(fieldName);
				values = dataMapping.get(fieldName);
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
				permExpression = handleColumnDataPermCondition(table, permExpression, condition);
			}
			
			if(logger.isTraceEnabled()) {
				logger.trace("_mybatis_sqlRewrite_trace processColumn ->table:{},column:{},addtionalValues:{}",table.getName(),column,values);
			}
		}
		
		//
		if(orConditionGroups != null) {
			for (List<ConditionPair> conditions : orConditionGroups) {
				permExpression = handleColumnDataPermCondition(table, permExpression, conditions);
			}
		}
		
		//数据owner
		AuthUser currentUser = null;
				if(withPermission 
						&& (currentUser = CurrentRuntimeContext.getCurrentUser()) != null 
						&& ( (defaultHandleOwner && strategy == null) || (strategy != null && strategy.handleOwner(table.getName()) )
				)) {
			if (userOwnerColumns == null || userOwnerColumns.length == 0) {
				userOwnerColumns = new String[] { createdByColumnName };
			}
			Expression userScopeExpression;
			boolean ignoreExistExpression = !withPermission; // 无其他权限时 permExpression 只有租户条件
			for (String scopeColumn : userOwnerColumns) {
				// 不存在该列
				if (!MetadataHelper.getTableColumnMappers(table.getName()).stream()
						.anyMatch(o -> o.getColumn().equals(scopeColumn))) {
					continue;
				}
				userScopeExpression = buildCurrentUserDataPermCondition(table, scopeColumn, currentUser,
						currentTenantId);
				// 无其他字段数据权限：租户 AND 数据owner
				if (permExpression == null || ignoreExistExpression) {
					permExpression = userScopeExpression;
					ignoreExistExpression = false;
				} else {// 有其他字段数据权限：(租户 AND 数据权限) OR (租户 AND 数据owner)
					permExpression = new OrExpression(new Parenthesis(permExpression), userScopeExpression);
				}
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

	private  Expression handleColumnDataPermCondition(Table table
			,Expression orginExpression
			,ConditionPair condition){
		Column column = new Column(table, condition.getColumn());
		String[] values = condition.getValues();
		//为空直接返回一个不成立的查询条件
		if(values == null || values.length == 0) {
			EqualsTo equalsTo = new EqualsTo();
			equalsTo.setLeftExpression(column);
			equalsTo.setRightExpression(new StringValue("__DATA_PERMISSION_NULL__"));
			return equalsTo;
		}
		Expression newExpression = orginExpression;
		if (values.length == 1) {
			if(values[0].endsWith(SpecialPermType.allDept.name()) 
					&& condition.getColumn().equals(deptColumnName)) {
				return orginExpression;
			}
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
				if(condition.getColumn().equalsIgnoreCase(softDeleteColumnName)) {
					newExpression = new AndExpression(orginExpression,expression);
				}else {
					newExpression = new AndExpression(expression,orginExpression);
				}
			}
		} else if (values.length > 1){
			if(orgPermFullCodeMode && condition.getColumn().equals(deptColumnName)) {
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
			EqualsTo equalsTo = new EqualsTo();
			equalsTo.setLeftExpression(column);
			equalsTo.setRightExpression(new StringValue("__DATA_PERMISSION_NULL__"));
			return equalsTo;
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
	
	private Expression buildCurrentUserDataPermCondition(Table table,String colomnName,AuthUser currentUser,String currentTenantId) {
		Expression expression;
		EqualsTo userEquals = new EqualsTo();
		userEquals.setLeftExpression(new Column(table, colomnName));
		userEquals.setRightExpression(new StringValue(currentUser.getId()));
		if(currentTenantId != null) {
			EqualsTo tenantEquals = new EqualsTo();
			tenantEquals.setLeftExpression(new Column(table, tenantColumnName));
			tenantEquals.setRightExpression(new StringValue(currentTenantId));
			expression = new Parenthesis(new AndExpression(tenantEquals, userEquals));
		}else {
			expression = userEquals;
		}
		return expression;
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
	
	private String getCurtrentDepartment() {
		if(organizationProvider != null)return organizationProvider.currentDepartment();
		organizationProvider = InstanceFactory.getInstance(OrganizationProvider.class);
		return organizationProvider == null ? null : organizationProvider.currentDepartment();
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
