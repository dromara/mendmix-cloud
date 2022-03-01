package com.jeesuite.mybatis.plugin.shard;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.GlobalConstants;
import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.metadata.MapperMetadata;
import com.jeesuite.mybatis.parser.MybatisMapperParser;
import com.jeesuite.mybatis.plugin.InvocationVals;
import com.jeesuite.mybatis.plugin.JeesuiteMybatisInterceptor;

/**
 * 分表自动路由
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 * @Copyright (c) 2015, jwww
 */
public class TableRouteHandler implements InterceptorHandler {

	protected static final Logger logger = LoggerFactory.getLogger(TableRouteHandler.class);

	private List<String> tableRouteMappedStatements = new ArrayList<>();
	
	@Override
	public void start(JeesuiteMybatisInterceptor context) {
		List<MapperMetadata> mappers = MybatisMapperParser.getMapperMetadatas(context.getGroupName());
		List<String> tmpTables = new ArrayList<>();
		for (MapperMetadata mapper : mappers) {
			if(!mapper.getTableName().contains(GlobalConstants.PLACEHOLDER_PREFIX))continue;
			tmpTables.add(mapper.getTableName());
		}
		for (MapperMetadata mapper : mappers) {
			if(tmpTables.contains(mapper.getTableName())) {
				tableRouteMappedStatements.add(mapper.getMapperClass().getName());
			}else {
				Set<String> querys = mapper.getQueryTableMappings().keySet();
				List<String> tables;
				for (String query : querys) {
					tables = mapper.getQueryTableMappings().get(query);
					for (String table : tables) {
						if(tmpTables.contains(table)) {
							tableRouteMappedStatements.add(query);
							break;
						}
					}
				}
			}
		}
	}
	
	@Override
	public Object onInterceptor(InvocationVals invocation) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onFinished(InvocationVals invocation, Object result) {}

	

	@Override
	public void close() {}

	@Override
	public int interceptorOrder() {
		return 0;
	}
} 

