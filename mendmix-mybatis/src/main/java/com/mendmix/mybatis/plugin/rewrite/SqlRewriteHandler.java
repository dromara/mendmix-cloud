/*
 * Copyright 2016-2020 www.jeesuite.com.
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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.ThreadLocalContext;
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
import com.mendmix.mybatis.crud.CrudMethods;
import com.mendmix.mybatis.kit.MybatisSqlUtils;
import com.mendmix.mybatis.metadata.ColumnMetadata;
import com.mendmix.mybatis.metadata.MapperMetadata;
import com.mendmix.mybatis.parser.MybatisMapperParser;
import com.mendmix.mybatis.plugin.InvocationVals;
import com.mendmix.mybatis.plugin.MendmixMybatisInterceptor;

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
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.update.Update;

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

	private static final String ARRAY_START = "[";
	private static final String QUERY_FUZZY_CHAR = "%";
	private static final String SQL_CACHE_KEY= "__SQL_CACHE__";
	public static final String FRCH_PREFIX = "__frch_";
	private static final String FRCH_INDEX_PREFIX = "__frch_index_";
	private static final String FRCH_ITEM_PREFIX = "__frch_item_";
	private static EqualsTo noPermssionCondition = new EqualsTo();
	
	//<table,<alias,[column...]>>
	private Map<String, LinkedHashMap<String,List<String>>> tableDataPermColumnMappings = new HashMap<>();
	//
	private List<String[]> globalOrRelationColumns = new ArrayList<>();
	private Map<String, List<String[]>> tableOrRelationColumns = new HashMap<>();
	private List<String> softDeleteMappedStatements = new ArrayList<>();
	private List<String> deptMappedStatements = new ArrayList<>();
	
	private boolean dynaDataPermEnaled;
	
	private String softDeleteColumn;
	private String softDeletePropName;
	private String softDeleteFalseValue;
	
	private boolean isFieldSharddingTenant;
	private String tenantColumnName;
	private String tenantPropName;
	
	private String deptColumnName;
	private String deptPropName;

	private String createdByColumnName;

	@Override
	public void start(MendmixMybatisInterceptor context) {
		
		noPermssionCondition = new EqualsTo();
		noPermssionCondition.setLeftExpression(new Column("1"));
		noPermssionCondition.setRightExpression(new LongValue(2));
	
		dynaDataPermEnaled = MybatisConfigs.isDataPermissionEnabled(context.getGroupName());
		isFieldSharddingTenant = MybatisConfigs.isColumnSharddingTenant(context.getGroupName());
		softDeleteColumn = MybatisConfigs.getSoftDeleteColumn(context.getGroupName());
		softDeleteFalseValue = MybatisConfigs.getSoftDeletedFalseValue(context.getGroupName());
		deptColumnName = MybatisConfigs.getDeptColumnName(context.getGroupName());
		createdByColumnName = MybatisConfigs.getCreatedByColumnName(context.getGroupName());
		
		final List<MapperMetadata> mappers = MybatisMapperParser.getMapperMetadatas(context.getGroupName());
		
		if(dynaDataPermEnaled) {
			Properties properties = ResourceUtils.getAllProperties("application.mybatis.dataPermission.columns[");
			properties.forEach( (k,v) -> {
				String tableName = k.toString().substring(k.toString().indexOf(ARRAY_START) + 1).replace("]", "").trim();
				buildTableDataPermColumnMapping(tableName, v.toString());
				
			} );
			//全局映射配置
			Map<String, String> globalDataPermColumnMappings = buildGlobalDataPermColumnMapping();
			for (MapperMetadata mapper : mappers) {
				Collection<String> columns = mapper.getPropToColumnMappings().values();
				Optional<Entry<String, String>> optional = globalDataPermColumnMappings.entrySet().stream().filter(e -> columns.contains(e.getValue())).findFirst();
			    if(optional.isPresent()) {
			    	addTableDataPermColumnMappings(mapper.getTableName(), optional.get().getKey(), optional.get().getValue());
			    }
			}
			//or group
			properties = ResourceUtils.getAllProperties("application.mybatis.dataPermission.orRelationColumns");
			properties.forEach( (k,v) -> {
				String tableName = null;
				String[] groupValues = StringUtils.split(v.toString(), ";");
				if(k.toString().contains(ARRAY_START)) {
					tableName = k.toString().substring(k.toString().indexOf(ARRAY_START) + 1).replace("]", "").trim();
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
		
		//软删除
		initColumnConfig(mappers, softDeleteColumn, null,softDeleteMappedStatements);
		//部门
		initColumnConfig(mappers, deptColumnName, null,deptMappedStatements);
		//字段隔离租户模式
		if (isFieldSharddingTenant) {
			tenantColumnName = MybatisConfigs.getTenantColumnName(context.getGroupName());

			ColumnMetadata tenantColumn;
			for (MapperMetadata mapper : mappers) {
				tenantColumn = mapper.getEntityMetadata().getColumns().stream().filter(o -> {
					return o.getColumn().equals(tenantColumnName);
				}).findFirst().orElse(null);

				if (tenantColumn == null)
					continue;
				if(tenantPropName == null)tenantPropName = tenantColumn.getProperty();
				//
				addTableDataPermColumnMappings(mapper.getTableName(), tenantPropName, tenantColumnName);
			}
		}
		
		StringBuilder logBuilder = new StringBuilder("ZVOS-FRAMEWORK-STARTUP-LOGGGING-->> \nsqlRewrite rules:");
		if(isFieldSharddingTenant)logBuilder.append("\n - tenantSharddingColumn:").append(tenantColumnName);
		if(deptColumnName != null)logBuilder.append("\n - deptColumnName:").append(deptColumnName);
		if(createdByColumnName != null)logBuilder.append("\n - createdByColumnName:").append(createdByColumnName);
		if(softDeleteColumn != null)logBuilder.append("\n - softDeleteColumn:").append(softDeleteColumn);
		if(softDeleteFalseValue != null)logBuilder.append("\n - softDeleteFalseValue:").append(softDeleteFalseValue);
		logBuilder.append("\n - tableDataPermColumnMappings:").append(tableDataPermColumnMappings);
		logBuilder.append("\n - globalOrRelationColumns:").append(JsonUtils.toJson(globalOrRelationColumns));
		logBuilder.append("\n - tableOrRelationColumns:").append(JsonUtils.toJson(tableOrRelationColumns));
		logger.info(logBuilder.toString());
	}
	
	private void addTableDataPermColumnMappings(String tableName,String propName,String columnName) {
		if(!tableDataPermColumnMappings.containsKey(tableName)) {
    		tableDataPermColumnMappings.put(tableName, new LinkedHashMap<>());
    	}
		List<String> columns = tableDataPermColumnMappings.get(tableName).get(propName);
		if(columns == null) {
			columns = new ArrayList<>(2);
			tableDataPermColumnMappings.get(tableName).put(propName, columns);
		}
		columns.add(columnName);
	}
	
	private void initColumnConfig(List<MapperMetadata> mappers,String column,List<String> filterTables,List<String> mappedStatements) {
		if(column == null)return;
		List<String> tmpTables = new ArrayList<>();
		ColumnMetadata columnMetadata;
		for (MapperMetadata mapper : mappers) {
			if(filterTables != null && !filterTables.contains(mapper.getTableName())) {
				continue;
			}
			if(mapper.getEntityMetadata() == null) {
				continue;
			}
			Set<ColumnMetadata> columnsMapping = mapper.getEntityMetadata().getColumns();
			columnMetadata = columnsMapping.stream().filter(o -> o.getColumn().equals(column)).findFirst().orElse(null);
			if(columnMetadata == null) {
				continue;
			}
			if(column.equals(deptColumnName)) {
				deptPropName = columnMetadata.getProperty();
			}else if(column.equals(softDeleteColumn)) {
				softDeletePropName = columnMetadata.getProperty();
			}
			tmpTables.add(mapper.getTableName());
			addTableDataPermColumnMappings(mapper.getTableName(), columnMetadata.getProperty(), column);
		}
		//
		if(mappedStatements == null)return;
		for (MapperMetadata mapper : mappers) {
			if(tmpTables.contains(mapper.getTableName())) {
				mappedStatements.add(mapper.getMapperClass().getName());
			}else {
				Set<String> querys = mapper.getQueryTableMappings().keySet();
				List<String> tables;
				for (String query : querys) {
					tables = mapper.getQueryTableMappings().get(query);
					for (String table : tables) {
						if(tmpTables.contains(table)) {
							mappedStatements.add(query);
							break;
						}
					}
				}
			}
		}
	}

	@Override
	public Object onInterceptor(InvocationVals invocation) throws Throwable {
		SqlRewriteStrategy sqlRewriteStrategy = MybatisRuntimeContext.getSqlRewriteStrategy();
		if(sqlRewriteStrategy.isIgnoreAny())return null;
		if(invocation.isSelect()) {
			//分表
			boolean isTableSharding = sqlRewriteStrategy.getRewritedTableMapping() != null && !sqlRewriteStrategy.getRewritedTableMapping().isEmpty();
			if(!isTableSharding && invocation.isSelectByPrimaryKey()) {
				return null;
			}
			RewriteSqlOnceContext context;
			context = new RewriteSqlOnceContext(invocation,isFieldSharddingTenant, dynaDataPermEnaled);
			//
			rewriteSelectSql(context);
			//分页的场景由分页组件执行查询
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
				//执行重写sql查询
				Executor executor = invocation.getExecutor();
				MappedStatement mappedStatement = invocation.getMappedStatement();
				ResultHandler<?> resultHandler = (ResultHandler<?>) invocation.getArgs()[3];
				List<ParameterMapping> parameterMappings = invocation.getBoundSql().getParameterMappings();
				BoundSql newBoundSql = new BoundSql(mappedStatement.getConfiguration(), invocation.getSql(),parameterMappings, invocation.getParameter());
				copyAdditionalParameters(invocation.getBoundSql(), newBoundSql);
				CacheKey cacheKey = executor.createCacheKey(mappedStatement, invocation.getParameter(), RowBounds.DEFAULT, newBoundSql);

				List<?> resultList = executor.query(mappedStatement, invocation.getParameter(), RowBounds.DEFAULT, resultHandler, cacheKey,newBoundSql);
				return resultList;
			}
		}else if(rewriteUpdateSql(invocation)) {
			Executor executor = invocation.getExecutor();
			MappedStatement mt = invocation.getMappedStatement();
			List<ParameterMapping> parameterMappings = invocation.getBoundSql().getParameterMappings();
			BoundSql newBoundSql = new BoundSql(mt.getConfiguration(), invocation.getSql(),parameterMappings, invocation.getParameter());
			copyAdditionalParameters(invocation.getBoundSql(), newBoundSql);
			SqlSource sqlSource = new SqlSource() {
				@Override
				public BoundSql getBoundSql(Object parameterObject) {
					return newBoundSql;
				}
			};
			MappedStatement newMt = new MappedStatement.Builder(mt.getConfiguration(), mt.getId(), sqlSource, mt.getSqlCommandType()).build();
			int updated = executor.update(newMt, invocation.getParameter());
			return updated;
		}
		return null;
	}
	
   public static void copyAdditionalParameters(BoundSql originBoundSql, BoundSql newBoundSql) {
		List<ParameterMapping> parameterMappings = originBoundSql.getParameterMappings();
		Object additionalParamVal;
		int itemIndex = 0;
		String indexParamName;
		for (ParameterMapping parameterMapping : parameterMappings) {
			//#{accountIds[${index}]}
			if(parameterMapping.getProperty().contains(ARRAY_START)) {
				continue;
			}
			try {				
				additionalParamVal = originBoundSql.getAdditionalParameter(parameterMapping.getProperty());
			} catch (Exception e) {
				logger.warn(">> get_additionalParamVal_error parameterMappingProperty:{},error:{}",parameterMapping.getProperty(),e.getMessage());
				continue;
			}
			//__frch_item_0
			//__frch_id_0
			if(additionalParamVal == null && !parameterMapping.getProperty().startsWith(FRCH_PREFIX)) {
				continue;
			}
			newBoundSql.setAdditionalParameter(parameterMapping.getProperty(), additionalParamVal);
			if(parameterMapping.getProperty().startsWith(FRCH_ITEM_PREFIX)) {
				indexParamName = FRCH_INDEX_PREFIX + itemIndex;
				if(!newBoundSql.hasAdditionalParameter(indexParamName)) {					
					newBoundSql.setAdditionalParameter(indexParamName, itemIndex);
				}
				itemIndex++;
			}
		}
	}

	/**
	 * @param invocation
	 * @return
	 */
	private void rewriteSelectSql(RewriteSqlOnceContext context) {
		String orignSql = context.invocation.getSql();
		//针对同一sql如果已经重写过就直接返回(需要考虑租户切换)
		String sqlCacheKey = orignSql + CurrentRuntimeContext.getTenantId(); //
		String rewritedSql = getCachingRewritedSql(sqlCacheKey);
		if (StringUtils.isNotBlank(rewritedSql)) {
			context.invocation.setRewriteSql(rewritedSql);
			return;
		}
		context.handleSoftDelete = !context.strategy.isIgnoreSoftDelete() && (
				softDeleteMappedStatements.contains(context.invocation.getMapperNameSpace()) 
				|| softDeleteMappedStatements.contains(context.invocation.getMappedStatement().getId())
			);
		
		if(!context.withConditionReriteRule() && !context.withTableShardingRule()) {
			return;
		}
			
		SelectBody selectBody = parseSelectSql(orignSql);
		if(selectBody == null)return; //parse error
		
		List<String> permGroupKeys = context.handleDataPerm ? context.getPermGroupKeys() : null;
		if(permGroupKeys == null || permGroupKeys.isEmpty()) {
			handleSelectRewrite(context,selectBody,false);
		}else {
			boolean withAllPermGroup = permGroupKeys.size()== 1 && SpecialPermType._allValues.name().equals(permGroupKeys.get(0));
			for (String groupKey : permGroupKeys) {
				if(!withAllPermGroup) {
					//加载分组权限
					context.currentGroupKey = groupKey;
					context.loadedCurrentGroupPermData = false;
					handleSelectRewrite(context, selectBody,true);
				}else {	
					//不处理数据权限
					context.handleDataPerm = false;
					handleSelectRewrite(context, selectBody,true);
					context.handleDataPerm = true;
					break;
				}
			}
			//
			for (RewriteTable table : context.rewriteTables) {
				Expression mergeExpression = null;
				if(table.getGroupExpressions() == null)continue;
				for (Expression expression : table.getGroupExpressions()) {	
					expression = wrapParenthesis(expression);
					mergeExpression = mergeExpression == null 
							? expression 
							: new OrExpression(mergeExpression, expression);
				}
				mergeExpression = mergeFinalPermissionExpression(context, table, mergeExpression);
				table.updateConditionExpression(mergeExpression);
			}
		}
		//
		context.invocation.setRewriteSql(selectBody.toString());
		//add cache
		//包含重写表名不缓存
		if(context.rewriteTables != null && !context.rewriteTables.stream().anyMatch(o -> o.getRewritedTableName() != null)) {			
			getThreadSqlCache().put(sqlCacheKey, selectBody.toString());
		}
	}
	
	private boolean rewriteUpdateSql(InvocationVals invocation) {
		try {
			Map<String, String> tableNameMappings = MybatisRuntimeContext.getSqlRewriteStrategy().getRewritedTableMapping();
			boolean isTableSharding = tableNameMappings != null && !tableNameMappings.isEmpty();
			Map<String, String> rewriteColumns = getUpdateRewriteColumns(invocation);
			if(isTableSharding == false 
					&& (rewriteColumns == null || rewriteColumns.isEmpty())) {
				return false;
			}
			boolean rewrited = false;
			Statement statement = CCJSqlParserUtil.parse(invocation.getSql());
			Table table = null;
			String tableName;
			if(statement instanceof Update) {
				Update update = (Update) statement;
				table = update.getTable();
				tableName = StringUtils.remove(table.getName(), RewriteTable.nameDelimiter);
				if(isTableSharding && tableNameMappings.containsKey(tableName)) {
					table.setName(tableNameMappings.get(tableName));
					rewrited = true;
				}
				Expression newWhere = handleUpdateColumnCondition(table, update.getWhere(), rewriteColumns);
				if(newWhere != null) {
					rewrited = true;
					update.setWhere(newWhere);
				}
			}else if(statement instanceof Delete) {
				Delete delete = (Delete) statement;
				table = delete.getTable();
				tableName = StringUtils.remove(table.getName(), RewriteTable.nameDelimiter);
				if(isTableSharding && tableNameMappings.containsKey(tableName)) {
					table.setName(tableNameMappings.get(tableName));
					rewrited = true;
				}
				Expression newWhere = handleUpdateColumnCondition(table, delete.getWhere(), rewriteColumns);
				if(newWhere != null) {
					rewrited = true;
					delete.setWhere(newWhere);
				}
			}else if(statement instanceof Insert) {
				Insert insert = (Insert) statement;
				table = insert.getTable();
				tableName = StringUtils.remove(table.getName(), RewriteTable.nameDelimiter);
				if(isTableSharding && tableNameMappings.containsKey(tableName)) {
					table.setName(tableNameMappings.get(tableName));
					rewrited = true;
				}
			}
			//
			if(rewrited) {
				invocation.setRewriteSql(statement.toString());
			}
			return rewrited;
		} catch (Exception e) {
			logger.error("<SQL_REWRITE_LOGGING> PARSER_SQL_ERROR \n -sql:{} -\n -reason:{}",invocation.getSql(),ExceptionUtils.getRootCause(e));
		}
		return false;
	}
	
	
	private SelectBody parseSelectSql(String sql) {
		//TODO cache ,deep clone
		SelectBody selectBody = null;
		try {
			Statement stmt = CCJSqlParserUtil.parse(sql);
			selectBody = ((Select)stmt).getSelectBody();
		} catch (JSQLParserException e) {
			try {				
				sql = MybatisSqlUtils.cleanSql(sql);
				Statement stmt = CCJSqlParserUtil.parse(sql);
				selectBody = ((Select)stmt).getSelectBody();
			} catch (JSQLParserException e2) {
				logger.error("<SQL_REWRITE_LOGGING> PARSER_SQL_ERROR \n -sql:{} -\n -reason:{}",sql,ExceptionUtils.getRootCause(e));
			}
		}
		return selectBody;
	}
	
	private Map<String, String> getUpdateRewriteColumns(InvocationVals invocation){
		MappedStatement mt = invocation.getMappedStatement();
		if(mt.getSqlCommandType().equals(SqlCommandType.INSERT)) {
			return null;
		}
		if(mt.getId().endsWith(CrudMethods.updateByPrimaryKey.name()) 
				|| mt.getId().endsWith(CrudMethods.updateByPrimaryKeySelective.name())
				|| mt.getId().endsWith(CrudMethods.deleteByPrimaryKey.name())) {
			return null;
		}
		MapperMetadata mapperMeta = MybatisMapperParser.getMapperMetadata(invocation.getMapperNameSpace());
		Map<String, List<String>> rewriteColumns = tableDataPermColumnMappings.get(mapperMeta.getTableName());
		if(rewriteColumns == null || rewriteColumns.isEmpty())return null;
		Map<String, String> map = new HashMap<>(2);
		if(rewriteColumns.containsKey(tenantPropName) 
				&& !MybatisRuntimeContext.getSqlRewriteStrategy().isIgnoreTenant()) {
			map.put(tenantPropName, rewriteColumns.get(tenantPropName).get(0));
		}
		if(rewriteColumns.containsKey(softDeleteColumn) 
				&& !MybatisRuntimeContext.getSqlRewriteStrategy().isIgnoreSoftDelete() 
				&& mt.getSqlCommandType().equals(SqlCommandType.UPDATE)) {
			map.put(softDeleteColumn, rewriteColumns.get(softDeleteColumn).get(0));
		}
		return map;
	}

	private String getCachingRewritedSql(String sqlCacheKey){
		if(!ThreadLocalContext.exists(SQL_CACHE_KEY))return null;
		return getThreadSqlCache().get(sqlCacheKey);
	}

	private Map<String, String> getThreadSqlCache() {
		Map<String, String> cache = ThreadLocalContext.get(SQL_CACHE_KEY);
		cache = Optional.ofNullable(cache).orElseGet(()-> {
			Map<String, String> map = new HashMap<>(5);
			ThreadLocalContext.set(SQL_CACHE_KEY, map);
			return map;
		});
		return cache;
	}


	private void handleSelectRewrite(RewriteSqlOnceContext context,SelectBody selectBody,boolean multiGroupMode) {
		InvocationVals invocation = context.invocation;
		if(selectBody instanceof PlainSelect) {
			PlainSelect select = (PlainSelect)selectBody;
			FromItem fromItem = select.getFromItem();
			if(fromItem instanceof Table) {
				if(context.rewriteTables == null) {
					context.parseRewriteTable(this,select);
				}
				//
				context.loadDataPermValues(this);
				List<RewriteTable> rewriteTables = context.rewriteTables;
				Expression newExpression;
				for (RewriteTable rewriteTable : rewriteTables) {
					if(!rewriteTable.hasRewriteColumn()) {
						continue;
					}
					newExpression = handleTableDataPermission(context,rewriteTable,!multiGroupMode);
					if(multiGroupMode) {
						rewriteTable.addGroupExpressions(newExpression);
					}else {						
						rewriteTable.updateConditionExpression(newExpression);
					}
				}
				//
				if(context.handleOrderBy) {
					handleTableOrderBy(select, (Table) select.getFromItem(), invocation);
				}	
			}else if(fromItem instanceof SubSelect) {
				SubSelect subSelect = (SubSelect) fromItem;
				handleSelectRewrite(context,subSelect.getSelectBody(),multiGroupMode);
			}
		}else if(selectBody instanceof SetOperationList) {
			SetOperationList optList = (SetOperationList) selectBody;
			List<SelectBody> selects = optList.getSelects();
			for (SelectBody body : selects) {
				if(selects.size() > 1)context.rewriteTables = null;
				handleSelectRewrite(context,body,multiGroupMode);
			}
		}
	}
	
	private Expression handleTableDataPermission(RewriteSqlOnceContext context,RewriteTable rewriteTable,boolean mergeConditon) {

		String tableName = rewriteTable.getTableName();
		SqlRewriteStrategy strategy = context.strategy;
		Map<String, String[]> dataMapping = context.getDataPermValues();
		Map<String, List<String>> rewriteColumnMapping = rewriteTable.rewriteColumnMapping;
		
		//定义的or条件列
		List<List<ConditionPair>> orConditionGroups = strategy == null ? null : strategy.getOrRelationColumns(tableName);
		if(orConditionGroups == null) {
			orConditionGroups = buildRelationColumns(tableName);
		}
		Expression permExpression = null;
		String[] values;
		Set<String> fieldNames = rewriteColumnMapping.keySet();
		boolean withPermission = false;
		boolean withAllPermission = false;
		String currentTenantId = null;
		ConditionPair condition = null;
		Table table = rewriteTable.getTable();
		for (String fieldName : fieldNames) {
			if(fieldName.equals(softDeletePropName)) {
				continue;
			}
			if(context.handleTenant && fieldName.equals(tenantPropName)) {
				if(context.mainTableHandledTenant && !rewriteTable.isAppendConditonUsingOn()) {
					continue;
				}
				currentTenantId = CurrentRuntimeContext.getTenantId();
				if(currentTenantId == null)throw new MendmixBaseException("无法获取当前租户ID[via:sqlRewrite]");
				values = new String[] {currentTenantId};
				condition = new ConditionPair(tenantColumnName, values);
				//
				if(!rewriteTable.isJoin()) {
					context.mainTableHandledTenant = true;
				}
			}else {
				if(dataMapping == null || !dataMapping.containsKey(fieldName))continue;
				List<String> columns = rewriteColumnMapping.get(fieldName);
				values = dataMapping.get(fieldName);
				if(values != null && values.length > 0 && SpecialPermType._allValues.name().equals(values[0])) {
					withAllPermission = true;
					continue;
				}
				
				boolean usedOrCondition = orConditionGroups != null;
				if(usedOrCondition) {
					conditionLoop:for (List<ConditionPair> conditions : orConditionGroups) {
						for (ConditionPair pair : conditions) {
							if(usedOrCondition = columns.contains(pair.getColumn())) {
								pair.setValues(values);
								break conditionLoop;
							}
						}
					}
				}
				if(!usedOrCondition) {
					if(columns.size() == 1) {
						condition = new ConditionPair(columns.get(0), values);
					}else {
						if(orConditionGroups == null) {
							orConditionGroups = new ArrayList<>(1);
						}
						List<ConditionPair> pairs = new ArrayList<>(columns.size());
						for (String column : columns) {
							pairs.add(new ConditionPair(column, values));
						}
						orConditionGroups.add(pairs);
					}
				}
				if(!withPermission)withPermission = true;
			}
			
			if(condition != null) {
				permExpression = handleColumnDataPermCondition(rewriteTable, permExpression, condition);
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
			Expression userScopeExpression = null;
			boolean ignoreExistExpression = !withPermission; //无其他权限时 permExpression 只有租户条件
			List<Expression> userScopeExpressions = ownerColumns.length == 1 ? null : new ArrayList<>(ownerColumns.length);
			for (String scopeColumn : ownerColumns) {
				userScopeExpression = buildCurrentUserDataPermCondition(context,rewriteTable, scopeColumn);
				if(ownerColumns.length > 1)userScopeExpressions.add(userScopeExpression);
			}
            //合并
			if(ownerColumns.length > 1) {
				Expression mergeExpression = null;
				for (Expression expression : userScopeExpressions) {
					if(mergeExpression == null) {
						mergeExpression = wrapParenthesis(expression);
					}else {
						mergeExpression = new OrExpression(mergeExpression, wrapParenthesis(expression));
					}
				}
				userScopeExpression = mergeExpression;
			}
			//无其他字段数据权限：租户 AND 数据owner
			if(permExpression == null || ignoreExistExpression) {
				permExpression = userScopeExpression;
				ignoreExistExpression = false;
			}else {
				if(MybatisConfigs.DATA_PERM_DEFAULT_HANDLE_OWNER) {
					//有其他字段数据权限：(租户 AND 数据权限) OR (租户 AND 数据owner)
					permExpression = new OrExpression(wrapParenthesis(permExpression), wrapParenthesis(userScopeExpression));
				}
			}
		}
		//合并条件
		if(mergeConditon) {
			permExpression = mergeFinalPermissionExpression(context, rewriteTable, permExpression);
		}
		//软删除
		if(context.handleSoftDelete && rewriteTable.rewriteColumnMapping.containsKey(softDeletePropName)) {
			EqualsTo equalsTo = new EqualsTo();
			equalsTo.setLeftExpression(new Column(rewriteTable.getTable(), softDeleteColumn));
			equalsTo.setRightExpression(new StringValue(softDeleteFalseValue));
			permExpression = permExpression == null ? equalsTo : new AndExpression(wrapParenthesis(permExpression), equalsTo);
		}
				
		if(context.traceLogging) {
			logger.info(">> 完成处理表：{}\n - 原条件:{}\n - 重写条件:{}",rewriteTable.getTableName()
							,rewriteTable.getOriginConditionExpression()
							,permExpression);
		}
				
		return permExpression;
	}

	private Expression mergeFinalPermissionExpression(RewriteSqlOnceContext context,RewriteTable rewriteTable,Expression permExpression) {
		 //原查询条件
		Expression whereExpression = rewriteTable.getOriginConditionExpression();
		if(whereExpression == null) {
			whereExpression = permExpression;
		}else if(permExpression != null) {
			whereExpression = new AndExpression(new Parenthesis(whereExpression),new Parenthesis(permExpression));
		}
		return whereExpression;
	}
	
	private  Expression handleColumnDataPermCondition(RewriteTable table
			,Expression orginExpression
			,ConditionPair condition){
		Column column = new Column(table.getTable(), condition.getColumn());
		String[] values = condition.getValues();
		if(values == null || values.length == 0) {
			if(!MybatisConfigs.DATA_PERM_STRICT_MODE) {
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
	
	/**
	 * 处理 租户，软删除等
	 * @param table
	 * @param orginWhere
	 * @param rewriteColumns
	 * @return
	 */
	private  Expression handleUpdateColumnCondition(Table table
			,Expression orginWhere
			,Map<String, String> rewriteColumns){
		Expression newWhere = null;
		String tenantId;
		if(rewriteColumns.containsKey(tenantPropName) && (tenantId = CurrentRuntimeContext.getTenantId()) != null) {
			EqualsTo expression = new EqualsTo();
			expression.setLeftExpression(new Column(table, tenantColumnName));
			expression.setRightExpression(new StringValue(tenantId));
			newWhere = new AndExpression(orginWhere,expression);
		}
		//
		if(rewriteColumns.containsKey(softDeletePropName)) {
			EqualsTo expression = new EqualsTo();
			expression.setLeftExpression(new Column(table, softDeleteColumn));
			expression.setRightExpression(new StringValue(softDeleteFalseValue));
			newWhere = new AndExpression(newWhere == null ? orginWhere : newWhere,expression);
		}
		return newWhere;
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
			if(!MybatisConfigs.DATA_PERM_STRICT_MODE) {
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
	

	private Expression buildCurrentUserDataPermCondition(RewriteSqlOnceContext context,RewriteTable table,String colomnName) {
		String currentUserId = context.currentUser.getId();
		Expression expression;
		EqualsTo userEquals = new EqualsTo();
		userEquals.setLeftExpression(new Column(table.getTable(), colomnName));
		userEquals.setRightExpression(new StringValue(currentUserId));
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
			if(orderBy == null)continue;
			String columnName = MybatisMapperParser.getMapperMetadata(invocation.getMapperNameSpace()).property2ColumnName(orderBy.getField());
    		if(columnName == null)columnName = orderBy.getField();
    		orderByElement = new OrderByElement();
    		orderByElement.setAsc(OrderType.ASC.name().equals(orderBy.getSortType()));
    		orderByElement.setExpression(new Column(table, columnName));
    		orderByElements.add(orderByElement);
		}
		
		selectBody.setOrderByElements(orderByElements);
	}
	
	
	public Map<String, List<String>> getTaleAllPermColumnMapping(String tableName){
		return tableDataPermColumnMappings.get(tableName);
	}
	
	public void mergeTableColumnMapping(Map<String, List<String>> columnMapping, String tableName) {
		if(!tableDataPermColumnMappings.containsKey(tableName)) {
			return;
		}
		LinkedHashMap<String, List<String>> map = tableDataPermColumnMappings.get(tableName);
		if(tenantPropName != null && map.containsKey(tenantPropName)) {
			columnMapping.put(tenantPropName, map.get(tenantPropName));
		}
		if(softDeletePropName != null && map.containsKey(softDeletePropName)) {
			columnMapping.put(softDeletePropName, map.get(softDeletePropName));
		}
	}

	private void buildTableDataPermColumnMapping(String tableName,String columnValue) {
		String[] columns = columnValue.split(",|;");
		String[] tmpArr;
		for (String column : columns) {
			tmpArr = column.split(":");
			String aliasName = tmpArr.length == 2 ? tmpArr[1] : StringConverter.toCamelCase(column);
			addTableDataPermColumnMappings(tableName,aliasName,tmpArr[0]);
		}
	}
	
	private Map<String,String> buildGlobalDataPermColumnMapping() {
		List<String> pairList = ResourceUtils.getList("application.mybatis.dataPermission.columns");
		Map<String,String> map = new HashMap<>(pairList.size());
		String[] tmpArr;
		for (String pair : pairList) {
			tmpArr = pair.split(":");
			String aliasName = tmpArr.length == 2 ? tmpArr[1] : StringConverter.toCamelCase(pair);
			map.put(aliasName,tmpArr[0]);
		}
		return map;
	}

	public String getDataPermColumnAlias(String table,String column) {
		String alias = null;
		if(tableDataPermColumnMappings.containsKey(table)) {
			alias = matchMapKeyByValue(tableDataPermColumnMappings.get(table),column);
		}
		if(alias == null) {
			alias = StringConverter.toCamelCase(column);
		}
		return alias;
	}
	
	public String getDeptColumnName() {
		return deptColumnName;
	}

	public String getDeptPropName() {
		return deptPropName;
	}

	public String getCreatedByColumnName() {
		return createdByColumnName;
	}
	
	private Expression wrapParenthesis(Expression expression) {
		if(expression instanceof Parenthesis) {
			return expression;
		}
		return new Parenthesis(expression);
	}

	private String matchMapKeyByValue(Map<String, List<String>> map,String expectValue) {
		if(map == null || map.isEmpty())return null;
		Optional<Entry<String, List<String>>> optional;
		optional = map.entrySet().stream().filter(
			e -> e.getValue().contains(expectValue)
		).findFirst();
		if(optional.isPresent()) {
			return optional.get().getKey();
		}
		return null;
	}
	
	public boolean matchRewriteStrategy(InvocationVals invocationVal,Object result) {
		MapperMetadata entityInfo = MybatisMapperParser.getMapperMetadata(invocationVal.getMapperNameSpace());
		if(entityInfo == null)return true;
		try {
			//租户判断
			String tenantId = CurrentRuntimeContext.getTenantId();
			if(tenantId != null 
					&& tenantPropName != null 
					&& !MybatisRuntimeContext.getSqlRewriteStrategy().isIgnoreTenant()
					&& !matchFieldValue(entityInfo, result, tenantPropName, tenantId)) {
				return false;
			}
			//软删除
			if(softDeletePropName != null 
					&& !MybatisRuntimeContext.getSqlRewriteStrategy().isIgnoreSoftDelete()
					&& !matchFieldValue(entityInfo, result, softDeletePropName, softDeleteFalseValue,Boolean.FALSE.toString())) {
				return false;
			}
			//TODO 数据权限
			
		} catch (Exception e) {
			logger.error("matchRewriteStrategy_error",e);
			return true;
		}
		
		return true;
	}
	
	private boolean matchFieldValue(MapperMetadata entityInfo,Object object,String fieldName,String...expectValues) {
		Object actualValue;
		if(entityInfo.getPropToColumnMappings().containsKey(fieldName)) {
			actualValue = CachingFieldUtils.readField(object, fieldName);
			if(actualValue == null || StringUtils.isBlank(actualValue.toString())) {
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
