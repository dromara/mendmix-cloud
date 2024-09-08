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
package org.dromara.mendmix.mybatis.plugin.shard;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.mybatis.MybatisRuntimeContext;
import org.dromara.mendmix.mybatis.kit.MybatisMapperParser;
import org.dromara.mendmix.mybatis.metadata.MapperMetadata;
import org.dromara.mendmix.mybatis.metadata.MapperMetadata.MapperMethod;
import org.dromara.mendmix.mybatis.plugin.MendmixMybatisInterceptor;
import org.dromara.mendmix.mybatis.plugin.OnceContextVal;
import org.dromara.mendmix.mybatis.plugin.PluginInterceptorHandler;
import org.dromara.mendmix.mybatis.plugin.shard.annotation.TableSharding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 分库自动路由处理
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2019年10月28日
 * @Copyright (c) 2015, jwww
 */
public class TableShardingHandler implements PluginInterceptorHandler {


	private static final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.mybatis");
	public static final String NAME = "tableSharding";
	
	private Map<String, TableShardingStrategy> tableStrategyMapping = new HashMap<>();
	private Map<String, Map<String, TableShardingStrategy>> methodStrategyMapping = new HashMap<>();
	
	@Override
	public void start(MendmixMybatisInterceptor context) {
		
		List<String> tableNames = ResourceUtils.getList("mendmix-cloud.mybatis.tableshard.tableNames");
		Class<? extends TableShardingStrategy> defStrategyClass = null;
		if(ResourceUtils.containsProperty("mendmix-cloud.mybatis.tableshard.defaultStrategy")) {
			try {
				defStrategyClass = (Class<? extends TableShardingStrategy>) Class.forName(ResourceUtils.getProperty("mendmix-cloud.mybatis.tableshard.defaultStrategy"));
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		
		List<MapperMetadata> entityInfos = MybatisMapperParser.getMapperMetadatas(context.getGroupName());
		
		Map<String, TableShardingStrategy> strategyInstances = new HashMap<>();
		Class<? extends TableShardingStrategy> strategyClass;
		TableShardingStrategy strategy;
		for (MapperMetadata ei : entityInfos) {
			if(!tableNames.contains(ei.getTableName()) && !ei.getEntityClass().isAnnotationPresent(TableSharding.class)) {
				continue;
			}
			if(ei.getEntityClass().isAnnotationPresent(TableSharding.class)) {
				strategyClass = ei.getEntityClass().getAnnotation(TableSharding.class).strategy();				
			}else {
				strategyClass = defStrategyClass;
			}
			if(strategyClass == null)continue;
			strategy = strategyInstances.get(strategyClass.getName());
			if(strategy == null) {
				try {
					strategy = strategyClass.newInstance();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				strategyInstances.put(strategyClass.getName(), strategy);
			}
			tableStrategyMapping.put(ei.getTableName(), strategy);
		}
		
		for (MapperMetadata ei : entityInfos) {		
			//关联查询
			Map<String, List<String>> queryTableMappings = ei.getQueryTableMappings();
			Set<String> querys = queryTableMappings.keySet();
			List<String> tables;
			for (String query : querys) {
				tables = ei.getQueryTableMappings().get(query);
				for (String table : tables) {
					if(!tableStrategyMapping.containsKey(table))continue;
					if(!methodStrategyMapping.containsKey(query)) {
						methodStrategyMapping.put(query, new HashMap<>());
					}
					methodStrategyMapping.get(query).put(table, tableStrategyMapping.get(table));
				}
			}
			//方法定义
			Collection<MapperMethod> methods = ei.getMapperMethods().values();
			for (MapperMethod method : methods) {
				if(method.getMethod().isAnnotationPresent(TableShardingScope.class)) {
					if(!methodStrategyMapping.containsKey(method.getFullName())) {
						methodStrategyMapping.put(method.getFullName(), new HashMap<>());
					}
					String[] defTables = method.getMethod().getAnnotation(TableShardingScope.class).tables();
					for (String table : defTables) {	
						if(tableStrategyMapping.containsKey(table)) {							
							methodStrategyMapping.get(method.getFullName()).put(table, tableStrategyMapping.get(table));
						}else {
							logger.warn(">>TableShardingStrategy NOT FOUND for:{}",table);
						}
					}
				}
			}
		}
		
		logger.info(">>TableSharding rules >> \ntable:\n -{} \nmethod:\n -{}",tableStrategyMapping.keySet(),methodStrategyMapping.keySet());
	}


	@Override
	public Object onInterceptor(OnceContextVal invocation) throws Throwable {
		//
		if(MybatisRuntimeContext.isIgnoreTableSharding()) {
			return null;
		}
        Map<String, String> tableNameMapping = null;
		TableShardingStrategy strategy;
        
		Map<String, TableShardingStrategy> map = methodStrategyMapping.get(invocation.getMappedStatement().getId());
	    if(map != null && !map.isEmpty()) {
	    	tableNameMapping = new HashMap<>(map.size());
	 	    for (String tableName : map.keySet()) {
	 	    	strategy = map.get(tableName);
	 			String rewritedTableName = strategy.buildShardingTableName(tableName, invocation);
	 	    	if(rewritedTableName != null && !rewritedTableName.equalsIgnoreCase(tableName)) {
	 	    		tableNameMapping.put(tableName, rewritedTableName);
	 	    	}
	 		}
	    }
	    //
		if(tableNameMapping == null) {
			tableNameMapping = new HashMap<>(1);
			MapperMetadata mapperMeta = invocation.getEntityInfo();
			if(mapperMeta != null && tableStrategyMapping.containsKey(mapperMeta.getTableName())) {
				strategy = tableStrategyMapping.get(mapperMeta.getTableName());
				String rewritedTableName = strategy.buildShardingTableName(mapperMeta.getTableName(), invocation);
				if(rewritedTableName != null && !rewritedTableName.equalsIgnoreCase(mapperMeta.getTableName())) {
					tableNameMapping.put(mapperMeta.getTableName(), rewritedTableName);
	 	    	}
			}
		}
		
		if(tableNameMapping != null && !tableNameMapping.isEmpty()) {
			if(logger.isDebugEnabled())logger.debug(">> Mapper[{}] matched tableSharding mapping:{}",invocation.getMappedStatement().getId(),tableNameMapping);
			invocation.setTableNameMapping(tableNameMapping);
		}
		
		return null;
	}

	@Override
	public void onFinished(OnceContextVal invocation, Object result) {}


	@Override
	public void close() {}

	@Override
	public int interceptorOrder() {
		return 1;
	}
} 

