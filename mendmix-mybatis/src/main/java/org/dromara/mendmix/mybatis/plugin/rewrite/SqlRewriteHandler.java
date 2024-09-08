/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.mybatis.plugin.rewrite;

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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.model.OrderBy;
import org.dromara.mendmix.common.model.OrderBy.OrderType;
import org.dromara.mendmix.common.model.PageParams;
import org.dromara.mendmix.common.util.CachingFieldUtils;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.common.util.StringConverter;
import org.dromara.mendmix.mybatis.DeptPermType;
import org.dromara.mendmix.mybatis.MybatisConfigs;
import org.dromara.mendmix.mybatis.MybatisRuntimeContext;
import org.dromara.mendmix.mybatis.crud.CrudMethods;
import org.dromara.mendmix.mybatis.kit.MybatisMapperParser;
import org.dromara.mendmix.mybatis.kit.MybatisSqlRewriteUtils;
import org.dromara.mendmix.mybatis.metadata.ColumnMetadata;
import org.dromara.mendmix.mybatis.metadata.MapperMetadata;
import org.dromara.mendmix.mybatis.plugin.MendmixMybatisInterceptor;
import org.dromara.mendmix.mybatis.plugin.OnceContextVal;
import org.dromara.mendmix.mybatis.plugin.PluginInterceptorHandler;
import org.dromara.mendmix.mybatis.plugin.rewrite.annotation.ConditionWithSubSelect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
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
public class SqlRewriteHandler implements PluginInterceptorHandler {

	private final static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix");

	private static Pattern multiSqlSpiterPattern = Pattern.compile(";\\s{0,}(UPDATE|INSERT)\\s+",Pattern.CASE_INSENSITIVE);
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
	private boolean softDeleteFalseValueAsNumber;
	
	private boolean isFieldSharddingTenant;
	private String tenantColumnName;
	private String tenantPropName;
	
	private String bUnitColumnName;
	private String bUnitPropName;
	
	private String deptColumnName;
	private String deptPropName;

	private String createdByColumnName;
	
	//private boolean ignoreAllJoinTableDataPerm;
	//private List<String> ignoreDataPermJoinTables = new ArrayList<>();
	
	private List<PluginInterceptorHandler> compatibleHandlers;

	@Override
	public void start(MendmixMybatisInterceptor context) {
		compatibleHandlers = context.getInterceptorHandlers().stream().filter(o -> o.compatibleSqlRewrite()).collect(Collectors.toList());
		noPermssionCondition = new EqualsTo();
		noPermssionCondition.setLeftExpression(new Column("1"));
		noPermssionCondition.setRightExpression(new LongValue(2));
	
		dynaDataPermEnaled = MybatisConfigs.isDataPermissionEnabled(context.getGroupName());
		isFieldSharddingTenant = MybatisConfigs.isFieldSharddingTenant(context.getGroupName());
		softDeleteColumn = MybatisConfigs.getSoftDeleteColumn(context.getGroupName());
		softDeleteFalseValue = MybatisConfigs.getSoftDeletedFalseValue(context.getGroupName());
		deptColumnName = MybatisConfigs.getDeptColumnName(context.getGroupName());
		createdByColumnName = MybatisConfigs.getCreatedByColumnName(context.getGroupName());
		
		final List<MapperMetadata> mappers = MybatisMapperParser.getMapperMetadatas(context.getGroupName());
		
		if(dynaDataPermEnaled) {
			Properties properties = ResourceUtils.getAllProperties("mendmix-cloud.mybatis.dataPermission.columns[");
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
			properties = ResourceUtils.getAllProperties("mendmix-cloud.mybatis.dataPermission.orRelationColumns");
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
		//经营单元
		initColumnConfig(mappers, softDeleteColumn, null,null);
		//软删除
		initColumnConfig(mappers, softDeleteColumn, null,softDeleteMappedStatements);
		//部门
		initColumnConfig(mappers, deptColumnName, null,deptMappedStatements);
		//字段隔离租户模式
		if (isFieldSharddingTenant) {
			tenantColumnName = MybatisConfigs.getTenantColumnName(context.getGroupName());

			ColumnMetadata tenantColumn;
			for (MapperMetadata mapper : mappers) {
				tenantColumn = mapper.getEntityMetadata().getColumnsMapper().stream().filter(o -> {
					return o.getColumn().equalsIgnoreCase(tenantColumnName);
				}).findFirst().orElse(null);

				if (tenantColumn == null)
					continue;
				if(tenantPropName == null)tenantPropName = tenantColumn.getProperty();
				//
				addTableDataPermColumnMappings(mapper.getTableName(), tenantPropName, tenantColumnName);
			}
		}
		//
		if (softDeleteColumn != null && StringUtils.isNumeric(softDeleteFalseValue)) {
			for (MapperMetadata mapper : mappers) {
				Optional<ColumnMetadata> optional = mapper.getEntityMetadata().getColumnsMapper().stream().filter(o -> softDeleteColumn.equalsIgnoreCase(o.getColumn())).findFirst();
			    if(optional.isPresent()) {
			    	Class<?> javaType = optional.get().getJavaType();
			    	softDeleteFalseValueAsNumber = javaType != String.class && javaType != char.class;
			    	break;
			    }
			}
		}

		StringBuilder logBuilder = new StringBuilder("<startup-logging>  \nsqlRewrite rules:");
		if(isFieldSharddingTenant)logBuilder.append("\n - tenantSharddingColumn:").append(tenantColumnName).append(":").append(tenantPropName);
		if(deptColumnName != null)logBuilder.append("\n - deptColumnName:").append(deptColumnName).append(":").append(getDeptPropName());
		if(createdByColumnName != null)logBuilder.append("\n - createdByColumnName:").append(createdByColumnName);
		if(softDeleteColumn != null)logBuilder.append("\n - softDeleteColumn:").append(softDeleteColumn).append(":").append(softDeletePropName);
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
			Set<ColumnMetadata> columnsMapping = mapper.getEntityMetadata().getColumnsMapper();
			columnMetadata = columnsMapping.stream().filter(o -> o.getColumn().equals(column)).findFirst().orElse(null);
			if(columnMetadata == null) {
				continue;
			}
			if(column.equals(deptColumnName)) {
				deptPropName = columnMetadata.getProperty();
			}else if(column.equals(softDeleteColumn)) {
				softDeletePropName = columnMetadata.getProperty();
			}else if(bUnitColumnName.equals(column)) {
				bUnitPropName = columnMetadata.getProperty();
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
	public Object onInterceptor(OnceContextVal invocation) throws Throwable {
		if(MybatisRuntimeContext.isIgnoreSqlRewrite())return null;
		if(invocation.isSelect()) {
			//分表
			boolean isTableSharding = invocation.getTableNameMapping() != null && !invocation.getTableNameMapping().isEmpty();
			if(!isTableSharding && invocation.isSelectByPrimaryKey()) {
				return null;
			}
			RewriteSqlOnceContext context = buildRewriteSqlContext(invocation);
			//
			rewriteSelectSql(context);
			//分页的场景由分页组件执行查询
			if(invocation.getPageObject() != null)return null;
	        //不查数据库直接返回
			if(invocation.getSql() == null) {
				List<Object> list = new ArrayList<>(1);
				//
				MapperMetadata entityInfo = invocation.getEntityInfo();
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
			//这个处理不会进入后面的拦截器，所以特殊处理
			if(compatibleHandlers != null) {
				for (PluginInterceptorHandler handler : compatibleHandlers) {
					handler.onInterceptor(invocation);
				}
			}
			MappedStatement newMt = new MappedStatement.Builder(mt.getConfiguration(), mt.getId(), sqlSource, mt.getSqlCommandType()).build();
			int updated = executor.update(newMt, invocation.getParameter());
			if(compatibleHandlers != null) {
				for (PluginInterceptorHandler handler : compatibleHandlers) {
					handler.onFinished(invocation, updated);
				}
			}
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
		//针对同一sql如果已经重写过就直接返回(需要考虑租户切换和分表情况)
		String sqlCacheKey = null;
		String rewritedSql = null;
		if(!context.withTableShardingRule()) {
			sqlCacheKey = orignSql + CurrentRuntimeContext.getTenantId(); //
			rewritedSql = getCachingRewritedSql(sqlCacheKey);
		}
		if (StringUtils.isNotBlank(rewritedSql)) {
			context.invocation.setRewriteSql(rewritedSql);
			return;
		}
		if(!context.withConditionReriteRule() && !context.withTableShardingRule()) {
			return;
		}
			
		Statement statement = MybatisSqlRewriteUtils.parseSql(orignSql);
		if(statement == null)return; //parse error
		SelectBody selectBody = ((Select)statement).getSelectBody();
		
		List<String> permGroupKeys = context.handleDataPerm ? context.getPermGroupKeys() : null;
		if(permGroupKeys == null || permGroupKeys.isEmpty()) {
			handleSelectRewrite(context,selectBody,false,true);
		}else {
			boolean withAllPermGroup = permGroupKeys.size()== 1 && DeptPermType._ALL_.name().equals(permGroupKeys.get(0));
			for (String groupKey : permGroupKeys) {
				if(!withAllPermGroup) {
					//加载分组权限
					context.currentGroupKey = groupKey;
					context.loadedCurrentGroupPermData = false;
					handleSelectRewrite(context, selectBody,true,true);
				}else {	
					//不处理数据权限
					context.handleDataPerm = false;
					handleSelectRewrite(context, selectBody,true,true);
					context.handleDataPerm = true;
					break;
				}
			}
			//
			for (RewriteTable table : context.rewriteTables) {
				Expression mergeExpression = null;
				if(table.getGroupExpressions() == null)continue;
				table.setUsingGlobalOrgPerm(false); //TODO 先关闭全局组织权限处理
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
		if(sqlCacheKey != null && context.rewriteTables != null && !context.rewriteTables.stream().anyMatch(o -> o.getRewritedTableName() != null)) {			
			getThreadSqlCache().put(sqlCacheKey, selectBody.toString());
		}
	}
	
	private boolean rewriteUpdateSql(OnceContextVal invocation) {
		try {
			boolean isTableSharding = invocation.getTableNameMapping() != null && !invocation.getTableNameMapping().isEmpty();
			Map<String, String> rewriteColumns = getUpdateRewriteColumns(invocation);
			//无分表 && 无重写列
			if(isTableSharding == false 
					&& (rewriteColumns == null || rewriteColumns.isEmpty())) {
				return false;
			}
			if(StringUtils.isBlank(invocation.getSql()) || multiSqlSpiterPattern.matcher(invocation.getSql()).find()) {
				if(isTableSharding)logger.warn("\nwarn!!!\nwarn!!!\n方法[{}]无法解析SQL:{},忽略分表处理!!!!!!",invocation.getMappedStatement().getId(),invocation.getSql());
				return false;
			}
			boolean rewrited = false;
			Map<String, String> tableNameMappings = invocation.getTableNameMapping();
			
			Statement statement = MybatisSqlRewriteUtils.parseSql(invocation.getSql());
			if(statement == null) {
				return false;
			}
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
			logger.error(">> rewriteUpdateSql_ERROR \n -sql:{}\n -reason:{}",invocation.getSql(),ExceptionUtils.getRootCauseStackTrace(e));
			return false;
		}
	}
	
	
	private Map<String, String> getUpdateRewriteColumns(OnceContextVal invocation){
		MappedStatement mt = invocation.getMappedStatement();
		if(mt.getSqlCommandType().equals(SqlCommandType.INSERT)) {
			return null;
		}
		if(mt.getId().endsWith(CrudMethods.updateByPrimaryKey.name()) 
				|| mt.getId().endsWith(CrudMethods.updateByPrimaryKeySelective.name())
				|| mt.getId().endsWith(CrudMethods.deleteByPrimaryKey.name())) {
			return null;
		}
		MapperMetadata entityInfo = invocation.getEntityInfo();
		if(entityInfo == null)return null;
		Map<String, List<String>> rewriteColumns = tableDataPermColumnMappings.get(entityInfo.getTableName());
		if(rewriteColumns == null || rewriteColumns.isEmpty())return null;
		Map<String, String> map = new HashMap<>(2);
		if(rewriteColumns.containsKey(tenantPropName) 
				&& !MybatisRuntimeContext.isIgnoreTenantMode()) {
			map.put(tenantPropName, rewriteColumns.get(tenantPropName).get(0));
		}
		if(rewriteColumns.containsKey(softDeleteColumn) 
				&& !MybatisRuntimeContext.isIgnoreSoftDeleteConditon() 
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


	private void handleSelectRewrite(RewriteSqlOnceContext context,SelectBody selectBody,boolean multiGroupMode,boolean handleWhereSubselect) {
		OnceContextVal invocation = context.invocation;
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
				//TODO 多组模式暂时忽略org分组
				//组模式org权限
				if(!multiGroupMode && context.isHandleGroupOrgPermission()) {
					if(context.traceLogging) {
						logger.info("<trace_logging> 开始处理全局组织权限...");
					}
					List<Expression> expressions = new ArrayList<>(rewriteTables.size());
					for (RewriteTable rewriteTable : rewriteTables) {
						if(!rewriteTable.isUsingGlobalOrgPerm())continue;
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
						logger.info("<trace_logging> 完成处理全局组织权限,重写新增条件:{}",mergeExpression);
					}
				}
				//
				if(context.handleOrderBy) {
					handleTableOrderBy(select, (Table) select.getFromItem(), invocation);
				}	
			}else if(fromItem instanceof SubSelect) {
				SubSelect subSelect = (SubSelect) fromItem;
				handleSelectRewrite(context,subSelect.getSelectBody(),multiGroupMode,true);
			}
			//处理where条件的子查询
			if(handleWhereSubselect && MapperMetadata.getAnnotation(invocation, ConditionWithSubSelect.class) != null) {				
				handleWhereSubSelect(invocation, select.getWhere(), multiGroupMode);
			}
		}else if(selectBody instanceof SetOperationList) {
			SetOperationList optList = (SetOperationList) selectBody;
			List<SelectBody> selects = optList.getSelects();
			for (SelectBody body : selects) {
				if(selects.size() > 1) {
					context.rewriteTables = null;
					context.unionSelect = true;
				}
				handleSelectRewrite(context,body,multiGroupMode,true);
				
			}
		}
	}
	
	private void handleWhereSubSelect(OnceContextVal invocation,Expression expression,boolean multiGroupMode) {
		if(expression instanceof Parenthesis) {
			expression = ((Parenthesis)expression).getExpression();
		}
		if(expression instanceof BinaryExpression) {
			Expression leftExpression = ((BinaryExpression)expression).getLeftExpression();
			handleWhereSubSelect(invocation,leftExpression,multiGroupMode);
			Expression rightExpression = ((BinaryExpression)expression).getRightExpression();
			handleWhereSubSelect(invocation,rightExpression,multiGroupMode);
		}else if(expression instanceof InExpression) {
			Expression leftExpression = ((InExpression)expression).getLeftExpression();
			handleWhereSubSelect(invocation,leftExpression,multiGroupMode);
			Expression rightExpression = ((InExpression)expression).getRightExpression();
			handleWhereSubSelect(invocation,rightExpression,multiGroupMode);
		}else if(expression instanceof ExistsExpression) {
			Expression rightExpression = ((ExistsExpression)expression).getRightExpression();
			handleWhereSubSelect(invocation,rightExpression,multiGroupMode);
		}else if(expression instanceof SubSelect){
			RewriteSqlOnceContext context = buildRewriteSqlContext(invocation);
			SubSelect subSelect = (SubSelect) expression;
			if(context.traceLogging) {
				logger.info("<trace_logging> handleWhereSubSelect:{}",expression);
			}
			handleSelectRewrite(context,subSelect.getSelectBody(),multiGroupMode,false);
		}
	}
	
	private Expression handleTableDataPermission(RewriteSqlOnceContext context,RewriteTable rewriteTable,boolean mergeConditon) {
		Expression permExpression = null;
		String tableName = rewriteTable.getTableName();
		DataPermissionStrategy strategy = context.strategy;
		Map<String, String[]> dataMapping = context.getDataPermValues();
		Map<String, List<String>> rewriteColumnMapping = rewriteTable.rewriteColumnMapping;
		Set<String> fieldNames = rewriteColumnMapping.keySet();
		//未登录
        if(context.handleDataPerm && context.currentUser == null) {
        	if(context.traceLogging)logger.info("<trace_logging> <处理列权限> currentUser为空,usingCondition: 1=2");
			return noPermssionCondition;
		}
      //未包含任何数据权限
       if(context.handleDataPerm && MybatisConfigs.DATA_PERM_STRICT_MODE 
      				&& (dataMapping == null || dataMapping.isEmpty()) 
      				&& (rewriteTable.getOwnerColumns() == null || rewriteTable.getOwnerColumns().length == 0)
		) {
			permExpression = noPermssionCondition;
			if (mergeConditon) {
				permExpression = mergeFinalPermissionExpression(context, rewriteTable, permExpression);
			}
			if (context.traceLogging)logger.info("<trace_logging> <处理列权限> 无任何数据权限,usingCondition: 1=2");
			return permExpression;
		}
		
		//定义的or条件列
		List<List<ConditionPair>> orConditionGroups = strategy == null ? null : strategy.getOrRelationColumns(tableName);
		if(orConditionGroups == null) {
			orConditionGroups = buildRelationColumns(tableName);
		}
		String[] values;
		boolean withPermission = false;
		boolean withAllPermission = false;
		String currentTenantId = null;
		ConditionPair condition = null;
		Table table = rewriteTable.getTable();
		for (String fieldName : fieldNames) {
			if(fieldName.equals(softDeletePropName)) {
				continue;
			}
			if(rewriteTable.isUsingGlobalOrgPerm() && fieldName.equals(deptPropName)) {
				continue;
			}
			condition = null;
			if(context.handleTenant && fieldName.equals(tenantPropName)) {
				if(context.mainTableHandledTenant && !rewriteTable.isAppendConditonUsingOn()) {
					continue;
				}
				currentTenantId = CurrentRuntimeContext.getTenantId();
				if(currentTenantId == null)throw new MendmixBaseException("无法获取当前租户ID[via:sqlRewrite]");
				values = new String[] {currentTenantId};
				condition = new ConditionPair(tenantColumnName, values);
				//
				if(!rewriteTable.isJoin() && !context.unionSelect) {
					context.mainTableHandledTenant = true;
				}
			}else if(fieldName.equals(bUnitPropName)){
				String businessUnitId = CurrentRuntimeContext.getBusinessUnitId();
				if(businessUnitId == null)continue;
				values = new String[] {businessUnitId};
				condition = new ConditionPair(bUnitColumnName, values);
			}else {
				if(dataMapping == null || !dataMapping.containsKey(fieldName))continue;
				List<String> columns = rewriteColumnMapping.get(fieldName);
				values = dataMapping.get(fieldName);
				if(values != null && values.length > 0 && DeptPermType._ALL_.name().equals(values[0])) {
					withAllPermission = true;
					continue;
				}
				
				boolean usedOrCondition = orConditionGroups != null;
				if(usedOrCondition) {
					boolean leastColumnMatched = false;
					for (List<ConditionPair> conditions : orConditionGroups) {
						for (ConditionPair pair : conditions) {
							if(columns.contains(pair.getColumn())) {
								pair.setValues(values);
								leastColumnMatched = true;
							}
						}
					}
					usedOrCondition = leastColumnMatched;
				}
				if(!usedOrCondition) {
					if(columns.size() == 1) {
						condition = new ConditionPair(columns.get(0), values);
					}else {
						//一个维度对应多个列的情况
						if(orConditionGroups == null) {
							orConditionGroups = new ArrayList<>(2);
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
		if(ownerColumns != null && ownerColumns.length > 0 && (rewriteTable.isOwnerUsingAnd() || !withAllPermission || withPermission)) {
			Expression userScopeExpression = null;
			boolean ignoreExistExpression = !withPermission; //无其他权限时 permExpression 只有租户条件
			List<Expression> userScopeExpressions = ownerColumns.length == 1 ? null : new ArrayList<>(ownerColumns.length);
			for (String scopeColumn : ownerColumns) {
				if(rewriteTable.isUsingGlobalOrgPerm() && !createdByColumnName.equals(scopeColumn)) {
					continue;
				}
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
				if(rewriteTable.isOwnerUsingAnd()) {
					permExpression = new AndExpression(wrapParenthesis(permExpression), wrapParenthesis(userScopeExpression));
				}else {					
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
			if(softDeleteFalseValueAsNumber) {
				equalsTo.setRightExpression(new LongValue(softDeleteFalseValue));
			}else {				
				equalsTo.setRightExpression(new StringValue(softDeleteFalseValue));
			}
			permExpression = permExpression == null ? equalsTo : new AndExpression(wrapParenthesis(permExpression), equalsTo);
		}
				
		if(context.traceLogging) {
			logger.info("<trace_logging> 完成处理表：{}\n - 原条件:{}\n - 重写条件:{}",rewriteTable.getTableName()
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
			if(rewriteTable.getTableStrategy() == null || rewriteTable.getTableStrategy().appendAfter()) {
				whereExpression = new AndExpression(new Parenthesis(whereExpression),new Parenthesis(permExpression));
			}else {				
				whereExpression = new AndExpression(new Parenthesis(permExpression),new Parenthesis(whereExpression));
			}
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
					if(value == null)continue;
					expressionList.getExpressions().add(new StringValue(value));
				}
				if(expressionList.getExpressions().isEmpty()) {
					newExpression = orginExpression;
					if(!MybatisConfigs.DATA_PERM_STRICT_MODE) {
						newExpression = orginExpression;
					}else {
						newExpression = noPermssionCondition;
					}
				}else {
					InExpression inExpression = new InExpression(column, expressionList);
					newExpression = orginExpression == null ? inExpression : new AndExpression(orginExpression,inExpression);
				}
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
		if(rewriteColumns == null || rewriteColumns.isEmpty())return orginWhere;
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
		}else if(orCount > 0) {
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
		if(context.currentUser != null && rewriteTable.getTableStrategy() != null) {
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
		String currentUserId = MybatisRuntimeContext.getCurrentUserId();
		if(currentUserId == null)throw new MendmixBaseException("当前用户id为空");
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
	
	private void handleTableOrderBy(PlainSelect selectBody, Table table, OnceContextVal invocation) {
		PageParams pageParam = invocation.getPageObject();
		List<OrderByElement> orderByElements = new ArrayList<>(pageParam.getOrderBys().size());
		
		OrderByElement orderByElement;
		for (OrderBy orderBy : pageParam.getOrderBys()) {
			if(orderBy == null || StringUtils.isBlank(orderBy.getField()))continue;
			MapperMetadata entityInfo = invocation.getEntityInfo();
			String columnName = entityInfo.property2ColumnName(orderBy.getField());
			if(columnName == null && entityInfo.getPropToColumnMappings().values().contains(orderBy.getField())) {
    			columnName = orderBy.getField();
    		}
			if(columnName == null) {
    			logger.warn(">>Column[{}] not found In table[{}]",orderBy.getField(),table.getName());
    			continue;
    		}
    		orderByElement = new OrderByElement();
    		orderByElement.setAsc(OrderType.ASC.name().equals(orderBy.getSortType()));
    		orderByElement.setExpression(new Column(table, columnName));
    		orderByElements.add(orderByElement);
		}
		if(!orderByElements.isEmpty()) {			
			selectBody.setOrderByElements(orderByElements);
		}
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
		if(bUnitPropName != null && map.containsKey(bUnitPropName)) {
			columnMapping.put(bUnitPropName, map.get(bUnitPropName));
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
		List<String> pairList = ResourceUtils.getList("mendmix-cloud.mybatis.dataPermission.columns");
		Map<String,String> map = new HashMap<>(pairList.size());
		String[] tmpArr;
		for (String pair : pairList) {
			tmpArr = pair.split(":");
			String aliasName = tmpArr.length == 2 ? tmpArr[1] : StringConverter.toCamelCase(pair);
			map.put(aliasName,tmpArr[0]);
		}
		return map;
	}
	
	private RewriteSqlOnceContext buildRewriteSqlContext(OnceContextVal invocation) {
		RewriteSqlOnceContext context = new RewriteSqlOnceContext(invocation,isFieldSharddingTenant, dynaDataPermEnaled);
		context.handleSoftDelete = !MybatisRuntimeContext.isIgnoreSoftDeleteConditon() && (
				softDeleteMappedStatements.contains(context.invocation.getMapperNameSpace()) 
				|| softDeleteMappedStatements.contains(context.invocation.getMappedStatement().getId())
			);
		return context;
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
		if(deptColumnName != null && deptPropName == null) {
			if(deptColumnName.toUpperCase().equals(deptColumnName)) {
				deptPropName = deptColumnName;
			}else {
				deptPropName = StringConverter.toCamelCase(deptColumnName);
			}
		}
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
	
	public boolean matchRewriteStrategy(OnceContextVal invocationVal,Object result) {
		MapperMetadata entityInfo = invocationVal.getEntityInfo();
		if(entityInfo == null)return true;
		try {
			//租户判断
			String tenantId = CurrentRuntimeContext.getTenantId();
			if(tenantId != null 
					&& tenantPropName != null 
					&& !MybatisRuntimeContext.isIgnoreTenantMode()
					&& !matchFieldValue(entityInfo, result, tenantPropName, tenantId)) {
				if(CurrentRuntimeContext.isDebugMode()) {
					logger.info("<trace_logging> column[{}] not match value:{} !!!",tenantPropName,tenantId);
				}
				return false;
			}
			//软删除
			if(softDeletePropName != null 
					&& !MybatisRuntimeContext.isIgnoreSoftDeleteConditon()
					&& !matchFieldValue(entityInfo, result, softDeletePropName, softDeleteFalseValue,Boolean.FALSE.toString())) {
				if(CurrentRuntimeContext.isDebugMode()) {
					logger.info("<trace_logging> column[{}] is deleted value!!",softDeletePropName);
				}
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
			//全局数据（虚拟租户）
			if(StringUtils.equals(fieldName, tenantPropName)
					&& GlobalConstants.VIRTUAL_TENANT_ID.equals(actualValue.toString())) {
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
	public void onFinished(OnceContextVal invocation, Object result) {
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
