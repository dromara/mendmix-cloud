package com.jeesuite.mybatis.plugin.shard;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.datasource.DataSourceContextHolder;
import com.jeesuite.mybatis.kit.ReflectUtils;
import com.jeesuite.mybatis.parser.EntityInfo;
import com.jeesuite.mybatis.parser.MybatisMapperParser;
import com.jeesuite.mybatis.plugin.JeesuiteMybatisInterceptor;

/**
 * 分库自动路由处理
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 * @Copyright (c) 2015, jwww
 */
public class DatabaseRouteHandler implements InterceptorHandler {


	protected static final Logger logger = LoggerFactory.getLogger(DatabaseRouteHandler.class);
	
	public static final String NAME = "dbShard";
	
	private static final String SPIT_POINT = ".";
	private static final String REGEX_BLANK = "\\n+\\s+";

	//分库策略
	private ShardStrategy<?> shardStrategy;
	
	private Pattern shardFieldAfterWherePattern;
	
	//忽略分库列表<mapperNameSpace>
	private List<String> ignoreTablesMapperNameSpace = new ArrayList<>();
	
	private List<String> ignoreMappedStatementIds = new ArrayList<>();
	
	//xml定义sql分库字段对应的参数名<mappedStatementId,paramName>
	private Map<String, String> shardFieldRalateParamNames = new HashMap<>();
	
	public void setShardStrategy(ShardStrategy<?> shardStrategy) {
		this.shardStrategy = shardStrategy;
	}

	@Override
	public Object onInterceptor(Invocation invocation) throws Throwable {

		Object[] objects = invocation.getArgs();
		MappedStatement ms = (MappedStatement) objects[0];
		Object parameterObject = objects[1];

		// TypeHandlerRegistry typeHandlerRegistry =
		// ms.getConfiguration().getTypeHandlerRegistry();
		
		if(ignoreMappedStatementIds.contains(ms.getId())){
			return null;
		}
		String namespace = ms.getId().substring(0, ms.getId().lastIndexOf(SPIT_POINT));
		//策略配置忽略
		if(ignoreTablesMapperNameSpace.contains(namespace)){
			return null;
		}

		BoundSql boundSql = ms.getBoundSql(parameterObject);
		
		Object parameterObject2 = boundSql.getParameterObject();
		System.out.println(parameterObject2);
	
		//是否需要分库
		boolean requiredShard = isRequiredShard(boundSql.getSql(), ms.getSqlCommandType(), namespace);
		
		if(requiredShard){
			//先检查是否已经设置
			Object shardFieldValue = getShardFieldValue(ms.getId(),parameterObject);
			if(shardFieldValue == null){
				logger.error("方法{}无法获取分库字段{}的值",ms.getId(),shardStrategy.shardEntityField());
			}else{				
				int dbIndex = shardStrategy.assigned(shardFieldValue);
				//指定数据库分库序列
				DataSourceContextHolder.get().setDbIndex(dbIndex);
			}
		}
		return null;
	}

	@Override
	public void onFinished(Invocation invocation, Object result) {

	}

	
	/**
	 * 判断该条sql是否需要分库
	 * @param sql
	 * @param cmdType
	 * @return
	 */
	private boolean isRequiredShard(String sql,SqlCommandType cmdType,String namespace){
		boolean isRequired = MybatisMapperParser.tableHasColumn(namespace, shardStrategy.shardDbField());
		//select方法 检查查询条件
		if(!isRequired && SqlCommandType.SELECT.equals(cmdType)){			
			sql = sql.replaceAll(REGEX_BLANK, "").toLowerCase();
			isRequired = shardFieldAfterWherePattern.matcher(sql).matches();
		}
		
		return isRequired;
	}
	
	/**
	 * 获取分库字段的值
	 * @param parameterObject
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Object getShardFieldValue(String mappedStatementId,Object parameterObject){
		try {			
			if(parameterObject == null || isSimpleDataType(parameterObject)){
				//TODO  按主键查询，删除??
				return null;
			} 
			if(parameterObject instanceof Map){
				Map<String, Object> map = (Map<String, Object>) parameterObject;
				String paramsName = shardFieldRalateParamNames.containsKey(mappedStatementId) ? shardFieldRalateParamNames.get(mappedStatementId) : shardStrategy.shardEntityField();
				return map.get(paramsName);
			}
			
			return ReflectUtils.getObjectValue(parameterObject, shardStrategy.shardEntityField());
		} catch (Exception e) {
			logger.error("解析分库字段["+shardStrategy.shardEntityField()+"]发生错误",e);
			return null;
		}
	}

	
	public static void main(String[] args) {
		String sql = "SELECT * FROM devices where a=2 and device_id = ?";
		System.out.println(sql.matches("^.*[WHERE|where|and|AND]\\s+device_id.*$"));
		
		sql = "( id,device_id,device_sn,device_type,device_name,create_time )";
		System.out.println(sql.matches("^.*,\\s*device_id\\s*,.*$"));
	}


	@Override
	public void start(JeesuiteMybatisInterceptor context) {
		
		List<EntityInfo> entityInfos = MybatisMapperParser.getEntityInfos();
		
		//TODO 解析mapper接口的DbShardKey标注
		for (EntityInfo entityInfo : entityInfos) {
			
		}
		
		shardFieldAfterWherePattern = Pattern.compile("^.*[WHERE|where|and|AND|ON|on]\\s+.*"+shardStrategy.shardDbField().toLowerCase()+".*$");
		
		//忽略分库表
		if(shardStrategy.ignoreTables() != null){
			//表名转小写
			List<String> ignoreTablesTmp = new ArrayList<>();
			for (String table : shardStrategy.ignoreTables()) {
				ignoreTablesTmp.add(table.toLowerCase());
			}
			for (EntityInfo entityInfo : entityInfos) {
				if(!ignoreTablesTmp.contains(entityInfo.getTableName().toLowerCase()))continue;
				ignoreTablesMapperNameSpace.add(entityInfo.getMapperClass().getName());
			}
		}
		
		//xml定义sql分库字段与属性名
		for (EntityInfo entityInfo : entityInfos) {
			Map<String, String> mapperSqls = entityInfo.getMapperSqls();
			for (String id : mapperSqls.keySet()) {
				String sql = mapperSqls.get(id).replaceAll("\\n+\\s+", "").replaceAll("(<\\!\\[CDATA\\[)|(\\]\\]>)", "");
				if(shardFieldAfterWherePattern.matcher(sql).matches()){
					//?TODO 解析非where
					String[] split = sql.split("[WHERE|where|and|AND|ON|on]\\s+.*"+shardStrategy.shardDbField().toLowerCase());
					String paramName = (split[split.length - 1]).trim().replaceAll("=|#|\\s+|\\{|\\}|<|>", "").split(REGEX_BLANK)[0];
					shardFieldRalateParamNames.put(id, paramName.trim());
				}else{
					
				}
			}
			
		}
	}
	
	
	private static boolean isSimpleDataType(Object o) {   
		   Class<? extends Object> clazz = o.getClass();
	       return 
	       (   
	           clazz.equals(String.class) ||   
	           clazz.equals(Integer.class)||   
	           clazz.equals(Byte.class) ||   
	           clazz.equals(Long.class) ||   
	           clazz.equals(Double.class) ||   
	           clazz.equals(Float.class) ||   
	           clazz.equals(Character.class) ||   
	           clazz.equals(Short.class) ||   
	           clazz.equals(BigDecimal.class) ||     
	           clazz.equals(Boolean.class) ||   
	           clazz.equals(Date.class) ||   
	           clazz.isPrimitive()   
	       );   
	   }


	@Override
	public void close() {
		
	}

	@Override
	public int interceptorOrder() {
		return 2;
	}

} 

