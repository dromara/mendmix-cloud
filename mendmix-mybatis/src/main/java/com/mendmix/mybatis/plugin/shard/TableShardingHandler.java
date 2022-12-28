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
package com.mendmix.mybatis.plugin.shard;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.mybatis.MybatisRuntimeContext;
import com.mendmix.mybatis.core.InterceptorHandler;
import com.mendmix.mybatis.metadata.MapperMetadata;
import com.mendmix.mybatis.metadata.MapperMetadata.MapperMethod;
import com.mendmix.mybatis.parser.MybatisMapperParser;
import com.mendmix.mybatis.plugin.InvocationVals;
import com.mendmix.mybatis.plugin.MendmixMybatisInterceptor;
import com.mendmix.mybatis.plugin.shard.annotation.TableSharding;
import com.mendmix.mybatis.plugin.shard.annotation.TableShardingScope;

/**
 * 分表自动路由
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Dec 22, 2022
 */
public class TableShardingHandler implements InterceptorHandler {

	private static final Logger logger = LoggerFactory.getLogger("com.mendmix.mybatis");
	
	public static final String NAME = "tableSharding";
	
	private Map<String, TableShardingStrategy> mapperStrategyMapping = new HashMap<>();
	private Map<String, Map<String, TableShardingStrategy>> methodStrategyMapping = new HashMap<>();
	
	@Override
	public void start(MendmixMybatisInterceptor context) {
		List<MapperMetadata> mappers = MybatisMapperParser.getMapperMetadatas(context.getGroupName());
		
		Map<String, TableShardingStrategy> strategyInstances = new HashMap<>();
		Map<String, TableShardingStrategy> tableStrategies = new HashMap<>();
		Class<? extends TableShardingStrategy> strategyClass;
		TableShardingStrategy strategy;
		for (MapperMetadata mapper : mappers) {
			if(!mapper.getEntityClass().isAnnotationPresent(TableSharding.class))continue;
			strategyClass = mapper.getEntityClass().getAnnotation(TableSharding.class).strategy();
			strategy = strategyInstances.get(strategyClass.getName());
			if(strategy == null) {
				try {
					strategy = strategyClass.newInstance();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				strategyInstances.put(strategyClass.getName(), strategy);
			}
			tableStrategies.put(mapper.getTableName(), strategy);
		}
		
		
		
		for (MapperMetadata mapper : mappers) {
			if(tableStrategies.containsKey(mapper.getTableName())) {
				strategy = tableStrategies.get(mapper.getTableName());
				mapperStrategyMapping.put(mapper.getMapperClass().getName(), strategy);
			}		
			//
			Map<String, List<String>> queryTableMappings = mapper.getQueryTableMappings();
			Set<String> querys = queryTableMappings.keySet();
			List<String> tables;
			for (String query : querys) {
				tables = mapper.getQueryTableMappings().get(query);
				tableLoop:for (String table : tables) {
					if(!tableStrategies.containsKey(table))continue tableLoop;
					if(!methodStrategyMapping.containsKey(query)) {
						methodStrategyMapping.put(query, new HashMap<>());
					}
					methodStrategyMapping.get(query).put(table, tableStrategies.get(mapper.getTableName()));
				}
			}
			//
			Collection<MapperMethod> methods = mapper.getMapperMethods().values();
			methodLoop:for (MapperMethod method : methods) {
				if(!method.getMethod().isAnnotationPresent(TableShardingScope.class)) {
					continue methodLoop;
				}
				String[] tables2 = method.getMethod().getAnnotation(TableShardingScope.class).tables();
				tableLoop:for (String table : tables2) {
					if(!tableStrategies.containsKey(table))continue tableLoop;
					if(!methodStrategyMapping.containsKey(method.getFullName())) {
						methodStrategyMapping.put(method.getFullName(), new HashMap<>());
					}
					methodStrategyMapping.get(method.getFullName()).put(table, tableStrategies.get(table));
				}
			}
		}
		
		logger.info("TableSharding \nmethodStrategyMapping:{}",methodStrategyMapping);
	}
	
	@Override
	public Object onInterceptor(InvocationVals invocation) throws Throwable {
		Map<String, String> tableNameMapping = null;
		TableShardingStrategy strategy;
		Map<String, TableShardingStrategy> map = methodStrategyMapping.get(invocation.getMappedStatement().getId());
	    if(map != null && !map.isEmpty()) {
	    	tableNameMapping = new HashMap<>(map.size());
		    for (String tableName : map.keySet()) {
		    	strategy = map.get(tableName);
				String rewritedTableName = strategy.buildShardingTableName(tableName, invocation);
		    	if(rewritedTableName != null)tableNameMapping.put(tableName, rewritedTableName);
			}
	    }
	    
	    
		if(tableNameMapping == null && mapperStrategyMapping.containsKey(invocation.getMapperNameSpace())) {
			tableNameMapping = new HashMap<>(1);
			MapperMetadata mapperMeta = MybatisMapperParser.getMapperMetadata(invocation.getMapperNameSpace());
			strategy = mapperStrategyMapping.get(invocation.getMapperNameSpace());
			String rewritedTableName = strategy.buildShardingTableName(mapperMeta.getTableName(), invocation);
			if(rewritedTableName != null)tableNameMapping.put(mapperMeta.getTableName(), rewritedTableName);
		}
		
		if(tableNameMapping != null && !tableNameMapping.isEmpty()) {
			MybatisRuntimeContext.getSqlRewriteStrategy().setRewritedTableMapping(tableNameMapping);
		}
		
		return null;
	}
	

	@Override
	public void onFinished(InvocationVals invocation, Object result) {}

	

	@Override
	public void close() {}

	@Override
	public int interceptorOrder() {
		return 1;
	}
} 

