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
package org.dromara.mendmix.mybatis.plugin.cache;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.async.StandardThreadExecutor.StandardThreadFactory;
import org.dromara.mendmix.common.model.AuthUser;
import org.dromara.mendmix.common.util.CachingFieldUtils;
import org.dromara.mendmix.common.util.DigestUtils;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.mybatis.MybatisConfigs;
import org.dromara.mendmix.mybatis.MybatisRuntimeContext;
import org.dromara.mendmix.mybatis.core.BaseEntity;
import org.dromara.mendmix.mybatis.crud.CrudMethods;
import org.dromara.mendmix.mybatis.exception.MybatisHanlerInitException;
import org.dromara.mendmix.mybatis.kit.CacheKeyUtils;
import org.dromara.mendmix.mybatis.kit.MybatisMapperParser;
import org.dromara.mendmix.mybatis.kit.MybatisSqlRewriteUtils;
import org.dromara.mendmix.mybatis.kit.MybatisSqlRewriteUtils.SqlMetadata;
import org.dromara.mendmix.mybatis.metadata.MapperMetadata;
import org.dromara.mendmix.mybatis.metadata.MapperMetadata.MapperMethod;
import org.dromara.mendmix.mybatis.plugin.MendmixMybatisInterceptor;
import org.dromara.mendmix.mybatis.plugin.OnceContextVal;
import org.dromara.mendmix.mybatis.plugin.PluginInterceptorHandler;
import org.dromara.mendmix.mybatis.plugin.cache.annotation.Cache;
import org.dromara.mendmix.mybatis.plugin.cache.annotation.CacheIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 自动缓存拦截处理
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 * @Copyright (c) 2015, jwww
 */
public class CacheHandler implements PluginInterceptorHandler {


	protected static final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.mybatis");

	public static final String CURRENT_USER_CONTEXT_NAME = "currentUser";
	private static final String BLOCK_ON_CONCURRENT_LOCK_RETURN = "_block_on_concurrentLock";
	public static final String NAME = "cache";
	public final static long IN_1MINS = 60;
    public final static long IN_1HOUR = 60 * 60;
	public static long defaultCacheExpire = 0;
	
	private static final String STR_PARAM = "param";
	
	public static final String GROUPKEY_SUFFIX = "~keys";
	private static final String ID_CACHEKEY_JOIN = ".id:";
	private boolean nullValueCache = ResourceUtils.getBoolean("mendmix-cloud.mybatis.cache.nullProtection");
	//null缓存占位符（避免频繁查询不存在对象造成缓存穿透导致频繁查询db）
	public static final String NULL_PLACEHOLDER = "~null";
	
	private static List<String> groupKeys = new ArrayList<>();
	
	//需要缓存的所有mapper
	private static List<String> cacheEnableMappers = new ArrayList<>();
	
	private static Map<String, Map<String, QueryCacheMethodMetadata>> queryCacheMethods = new HashMap<>();
	
	private static Map<String, UpdateByPkCacheMethodMetadata> updatePkCacheMethods = new HashMap<>();
	
	private static List<String> batchUpdateByPkCacheMethods = Arrays.asList("batchLogicDelete","batchUpdateByPrimaryKeys");
	
	//<更新方法msId,[关联查询方法列表]>
	private static Map<String, List<String>> customUpdateCacheMapppings = new HashMap<>();
	
	protected static CacheProvider cacheProvider;
	
	private static String dataSourceGroupName;
	
	private DataSource dataSource;
	
	private ExecutorService cleanCacheExecutor;
	
	
	@Override
	public void start(MendmixMybatisInterceptor context) {
		
		cleanCacheExecutor = new ThreadPoolExecutor(2, 20,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2000),
                new StandardThreadFactory("cleanCacheExecutor"));
		
		dataSourceGroupName = context.getGroupName();
		dataSource = context.getDataSource();
		
		cacheProvider = new CacheProvider(dataSourceGroupName);
		
		defaultCacheExpire = Long.parseLong(MybatisConfigs.getProperty(context.getGroupName(), MybatisConfigs.CACHE_EXPIRE_SECONDS, "0"));

		List<MapperMetadata> entityInfos = MybatisMapperParser.getMapperMetadatas(context.getGroupName());
		
		Class<BaseEntity> baseEntityClass = BaseEntity.class;
		QueryCacheMethodMetadata methodCache = null;
		for (MapperMetadata ei : entityInfos) {
			if(ei.getMapperClass().isAnnotationPresent(CacheIgnore.class))continue;
			if(!baseEntityClass.isAssignableFrom(ei.getEntityClass())){
				logger.warn("[{}] not extends from [{}],ignore register auto cache!!!!",ei.getEntityClass().getName(),baseEntityClass.getName());
				continue;
			}
			Class<?> mapperClass = ei.getMapperClass();
			
			//按主键查询方法定义
			QueryCacheMethodMetadata queryByPKMethod = generateQueryByPKMethod(mapperClass, ei.getEntityClass());
			if(queryByPKMethod == null)continue;
			Map<String, QueryCacheMethodMetadata> tmpMap = new HashMap<>();
			//主键查询方法
			tmpMap.put(queryByPKMethod.methodName, queryByPKMethod);
			
			String keyPatternForPK = queryByPKMethod.keyPattern;
			//接口定义的自动缓存方法
			for (MapperMethod method : ei.getMapperMethods().values()) {
				if(method.getMethod().isAnnotationPresent(Cache.class)){
					if(tmpMap.containsKey(method.getFullName()))continue;
					methodCache = generateQueryMethodCacheByMethod(ei, method);
					tmpMap.put(method.getFullName(), methodCache);
					logger.info("<startup-logging>  解析查询方法{}自动缓存配置 ok,keyPattern:[{}]",methodCache.methodName,methodCache.keyPattern);
				}
			}
			//缓存需要自动缓存的mapper
			cacheEnableMappers.add(ei.getMapperClass().getName());
			logger.info("<startup-logging>  解析查询方法{}自动缓存配置 ok,keyPattern:[{}]",queryByPKMethod.methodName,queryByPKMethod.keyPattern);
			
			queryCacheMethods.put(mapperClass.getName(), tmpMap);
			
			//更新缓存方法
			generateUpdateByPkCacheMethod(mapperClass, ei.getEntityClass(), keyPatternForPK);
		}
		//
		logger.info("<startup-logging>  customUpdateCacheMapppings:{}",customUpdateCacheMapppings);
	}

	@Override
	public Object onInterceptor(OnceContextVal invocationVal) throws Throwable {
		
		if(MybatisRuntimeContext.isIgnoreCache())return null;
		if(invocationVal.getPageObject() != null)return null;

		MappedStatement mt = invocationVal.getMappedStatement(); 

		boolean getLock = false;
		String cacheKey = null;
		if(mt.getSqlCommandType().equals(SqlCommandType.SELECT)){	
			//事务方法内部的查询不走缓存
			if(MybatisRuntimeContext.isTransactionalOn()){
				if(logger.isDebugEnabled())logger.debug(">>auto_cache_process skipCache[isTransactionalOn] -> mapperId:{}",mt.getId());
				return null;
			}
			//按主键查询
			QueryCacheMethodMetadata metadata = getQueryMethodCache(mt.getId());
			if(metadata == null) {
				return null;
			}
			
			invocationVal.setQueryCacheMetadata(metadata);
			cacheKey = genarateQueryCacheKey(invocationVal,metadata.keyPattern, invocationVal.getParameter());
			invocationVal.setCacheKey(cacheKey);
			
			//并发控制防止缓存穿透
			if(!metadata.concurrency){
				String concurrentLockKey = "concurrent:" + cacheKey;
				invocationVal.setConcurrentLockKey(concurrentLockKey);
				getLock = cacheProvider.setnx(concurrentLockKey, "1", 30);
				if(!getLock){
					if(logger.isDebugEnabled())logger.debug(">>auto_cache_process notGetConcurrentLock -> mapperId:{}",mt.getId());
					return BLOCK_ON_CONCURRENT_LOCK_RETURN;
				}
				if(logger.isDebugEnabled())logger.debug(">>auto_cache_process getConcurrentLock -> mapperId:{}",mt.getId());
			}
			
			Object cacheObject = null;
			boolean nullPlaceholder = false;
			//
			if(!metadata.isSecondQueryById()){
				//从缓存读取
				cacheObject = cacheProvider.get(cacheKey);
				nullPlaceholder = nullValueCache && NULL_PLACEHOLDER.equals(cacheObject);
				if(StringUtils.isNotBlank(metadata.refKey) && (nullPlaceholder || cacheObject == null)){
					cacheObject = cacheProvider.get(metadata.refKey);
					nullPlaceholder = nullValueCache && NULL_PLACEHOLDER.equals(cacheObject);
				}
				if(nullPlaceholder){
					if(logger.isDebugEnabled())logger.debug(">>auto_cache_process hitCache -> mapperId:{},cacheKey:[{}]",mt.getId(),cacheKey);
				}else if(cacheObject != null){
					if(logger.isDebugEnabled())logger.debug(">>auto_cache_process hitCache -> mapperId:{},cacheKey:[{}]",mt.getId(),cacheKey);
				}
			}else{
				//新根据缓存KEY找到与按ID缓存的KEY
				String refCacheKey = nullValueCache ? cacheProvider.get(cacheKey) : cacheProvider.getStr(cacheKey);
				if(refCacheKey != null){
					if(nullPlaceholder = (nullValueCache && NULL_PLACEHOLDER.equals(refCacheKey))){
						cacheObject = NULL_PLACEHOLDER;
					}else{						
						cacheObject = cacheProvider.get(refCacheKey);
						if(cacheObject != null && logger.isDebugEnabled())logger.debug(">>auto_cache_process  hitRefCache -> mapperId:{},cacheKey:[{}],refCacheKey:[{}]",mt.getId(),cacheKey,refCacheKey);
					}
				}
			}
			
			if(nullPlaceholder){
				cacheObject = new ArrayList<>(0);
			}else if(cacheObject != null && !(cacheObject instanceof Collection)){	
				List<Object> tmpList = new ArrayList<>(1);
				tmpList.add(cacheObject);
				cacheObject = tmpList;
			}
			
			return cacheObject;
		} 
		
		return null;
	
	}
	

	@SuppressWarnings("rawtypes")
	@Override
	public void onFinished(OnceContextVal invocationVal,Object result) {
		try {
			if(MybatisRuntimeContext.isIgnoreCache())return;
			if(invocationVal.getPageObject() != null)return;
			if(BLOCK_ON_CONCURRENT_LOCK_RETURN.equals(result))return;
			MappedStatement mt = invocationVal.getMappedStatement(); 
			
			QueryCacheMethodMetadata metadata = null;
			if(mt.getSqlCommandType().equals(SqlCommandType.SELECT)){	
				if(result == null)return; 
				if((metadata = invocationVal.getQueryMethodMetadata()) == null)return;
	
				final String cacheKey = invocationVal.getCacheKey();
				if(result instanceof List){
					List list = (List)result;
					if(list.isEmpty()){
						if(nullValueCache){
							cacheProvider.set(cacheKey,NULL_PLACEHOLDER, IN_1MINS);
						}
						return;
					}
					result = metadata.collectionResult ? result : list.get(0);
				}else if(nullValueCache && NULL_PLACEHOLDER.equals(result)) {
					cacheProvider.set(cacheKey,NULL_PLACEHOLDER, IN_1MINS);
					return;
				}
				//
				if(!metadata.isSecondQueryById()){
					cacheProvider.set(cacheKey,result, metadata.getExpire());
					if(logger.isDebugEnabled())logger.debug(">>auto_cache_process addCache -> mapperId:{},cacheKey:{}",mt.getId(),cacheKey);
					
					if(metadata.isPk){//唯一索引（业务上）
						cacheUniqueSelectRef(invocationVal,result, mt, cacheKey);
					}else if(metadata.groupRalated){//结果为集合的情况，增加key到cacheGroup
						cacheProvider.putGroup(metadata.cacheGroupKey, cacheKey);
					}
				}else{
					//之前没有按主键的缓存，增加按主键缓存
					String idCacheKey = genarateQueryCacheKey(invocationVal,getQueryByPkMethodCache(mt.getId()).keyPattern,result);
					
					if(idCacheKey != null && cacheKey != null){
						if(!cacheProvider.exists(idCacheKey)){						
							//缓存idkey->实体
							cacheProvider.set(idCacheKey,result, metadata.getExpire());
						}
						//缓存fieldkey->idkey
						cacheFieldRefKey(cacheKey,idCacheKey, metadata.getExpire());
						if(logger.isDebugEnabled())logger.debug(">>auto_cache_process addCache -> mapperId:{},idCacheKey:{},cacheKey:{}",mt.getId(),idCacheKey,cacheKey);
					}
				}
			}else{
				String mapperClassName = invocationVal.getMapperNameSpace();
				if(!cacheEnableMappers.contains(mapperClassName) && !customUpdateCacheMapppings.containsKey(mt.getId()))return;
				//返回0，未更新成功
				if(result != null && ((int)result) == 0)return;
				//
				if(mt.getSqlCommandType().equals(SqlCommandType.INSERT)) {
					//TODO 写入缓存 ，考虑回滚
				}else {
					if(updatePkCacheMethods.containsKey(mt.getId())){
						UpdateByPkCacheMethodMetadata updateMethodCache = updatePkCacheMethods.get(mt.getId());
						String idCacheKey = genarateQueryCacheKey(invocationVal,updateMethodCache.keyPattern,invocationVal.getParameter());
						cacheProvider.remove(idCacheKey);
						if(logger.isDebugEnabled())logger.debug(">>auto_cache_process removeCache -> mapperId:{},cacheKey:{}",mt.getId(),idCacheKey);
					} else if(batchUpdateByPkCacheMethods.contains(mt.getId().substring(mapperClassName.length() + 1))){
						List<String> idCacheKeys = buildBatchUpdateIdCacheKeys(mapperClassName, invocationVal.getParameter());
						if(!idCacheKeys.isEmpty())cacheProvider.remove(idCacheKeys.toArray(new String[0]));
						if(logger.isDebugEnabled())logger.debug(">>auto_cache_process removeCache -> mapperId:{},cacheKeys:{}",mt.getId(),idCacheKeys);
					} else{
						//针对按条件更新或者删除的方法，按查询条件查询相关内容，然后清理对应主键缓存内容
						MapperMetadata entityInfo = invocationVal.getEntityInfo();
						final Object parameter = invocationVal.getParameter();
						BoundSql boundSql = mt.getBoundSql(parameter);
						String orignSql = boundSql.getSql();
						String idColumn = entityInfo.getIdColumn();
						SqlMetadata sqlMetadata = MybatisSqlRewriteUtils.rewriteAsSelectPkField(orignSql, idColumn);
						//
						String tenantId = MybatisRuntimeContext.getCurrentTenant();
						cleanCacheExecutor.execute(new Runnable() {
							@Override
							public void run() {
								if(tenantId != null) {
									CurrentRuntimeContext.setTenantId(tenantId);
								}
								removeCacheByDyncQuery(entityInfo,boundSql, sqlMetadata);
							}
						});
					}
				}
				//删除同一cachegroup关联缓存
				removeCacheByGroup(mt.getId(), mapperClassName);
				//删除自定义关联缓存
				if(customUpdateCacheMapppings.containsKey(mt.getId())) {
					removeCustomRelateCache(mt.getId());
				}
			}
		} finally {
			//清除并发控制锁
			if(invocationVal.getConcurrentLockKey() != null){
				cacheProvider.remove(invocationVal.getConcurrentLockKey());
			}
		}
	}

	/**
	 * 缓存其他唯一结果查询方法和主键缓存的引用
	 * @param object
	 * @param mt
	 * @param cacheKey
	 */
	private void cacheUniqueSelectRef(OnceContextVal invocationVal,Object object, MappedStatement mt, String cacheKey) {
		Collection<QueryCacheMethodMetadata> mcs = queryCacheMethods.get(mt.getId().substring(0, mt.getId().lastIndexOf(OnceContextVal.DOT))).values();
		outter:for (QueryCacheMethodMetadata methodCache : mcs) {
			if(!methodCache.isSecondQueryById())continue;
			try {	
				Object[] cacheFieldValues = new Object[methodCache.fieldNames.length];
				for (int i = 0; i < cacheFieldValues.length; i++) {
					if(methodCache.fieldNames[i] == null)break outter;
					cacheFieldValues[i] = CachingFieldUtils.readField(object, methodCache.fieldNames[i]);
					if(cacheFieldValues[i] == null)continue outter;
				}
				String fieldCacheKey = genarateQueryCacheKey(invocationVal,methodCache.keyPattern , cacheFieldValues);
				
				cacheFieldRefKey(fieldCacheKey,cacheKey, methodCache.getExpire());
				if(logger.isDebugEnabled())logger.debug(">>auto_cache_process addRefCache -> mapperId:{},cacheKey:{},refkey:{}",mt.getId(),fieldCacheKey,cacheKey);
			} catch (Exception e) {
				logger.warn("cacheUniqueSelectRef:"+cacheKey,e);
			}
		}
	}
	
	/**
	 * 缓存字段查询到idkey
	 * @param fieldCacheKey
	 * @param idCacheKey
	 * @param expired
	 */
	private void cacheFieldRefKey(String fieldCacheKey,String idCacheKey,long expired){
		if(nullValueCache){
			cacheProvider.set(fieldCacheKey, idCacheKey, expired);
		}else{
			cacheProvider.setStr(fieldCacheKey, idCacheKey, expired);
		}
	}
	
	/**
	 * 根据动态查询内容清理缓存
	 * @param sqlMetadata 查询主键列表SQL语句信息
	 * @param parameter 参数
	 * @throws Exception 
	 */
	private void removeCacheByDyncQuery(MapperMetadata entityInfo,BoundSql boundSql,SqlMetadata sqlMetadata) {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try {
			MybatisSqlRewriteUtils.parseDyncQueryParameters(boundSql, sqlMetadata);
			connection = dataSource.getConnection();
			statement = connection.prepareStatement(sqlMetadata.getSql());
			
			List<Object> parameters = sqlMetadata.getParameters();
			for (int i = 0; i < parameters.size(); i++) {
				statement.setObject(i+1, parameters.get(i));
			}
			
			rs = statement.executeQuery();
			List<String> ids = new ArrayList<>();
			while (rs.next()) {
				ids.add(rs.getString(1));
			}
			if(ids != null && !ids.isEmpty()){
				List<String> idCacheKeys = ids.stream().map(id -> {
					return entityInfo.getEntityClass().getSimpleName() + ID_CACHEKEY_JOIN + id.toString();
				}).collect(Collectors.toList());
				cacheProvider.remove(idCacheKeys.toArray(new String[0]));
				if(logger.isDebugEnabled()) {
					logger.debug("remove cacheKeys:{}",idCacheKeys);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			final String groupName = entityInfo.getEntityClass().getSimpleName();
			cacheProvider.clearGroup(groupName);
		}finally {
			try {rs.close();} catch (Exception e2) {}
			try {statement.close();} catch (Exception e2) {}
			try {connection.close();} catch (Exception e2) {}
		}
	}
	
	
	
	/**
	 * 删除缓存组
	 * @param msId
	 * @param mapperClassName
	 * @param removePkCache  是否同时删除按主键的缓存
	 */
	private void removeCacheByGroup(String msId, String mapperClassName) {
		MapperMetadata entityInfo = MybatisMapperParser.getMapperMetadata(mapperClassName);
		if(entityInfo == null)return;
		final String groupName = entityInfo.getEntityClass().getSimpleName();
		cleanCacheExecutor.execute(new Runnable() {
			@Override
			public void run() {
				cacheProvider.clearGroup(groupName);
				if(logger.isDebugEnabled())logger.debug(">>auto_cache_process removeGroupCache -> mapperId:{},groupName:{}",msId,groupName);
			}
		});
	}
	
	/**
	 * 删除更新方法自定义缓存关系
	 * @param updateId
	 */
	private void removeCustomRelateCache(String updateId) {
		final List<String> queryMethods = customUpdateCacheMapppings.get(updateId);
		if(queryMethods == null || queryMethods.isEmpty())return;
		cleanCacheExecutor.execute(new Runnable() {
			@Override
			public void run() {
				QueryCacheMethodMetadata metadata;
				for (String method : queryMethods) {
					metadata = getQueryMethodCache(method);
					String prefix = StringUtils.splitByWholeSeparator(metadata.keyPattern, "%s")[0];
					if(logger.isDebugEnabled())logger.debug(">>auto_cache_process removeCustomRelateCache -> cacheGroupKey:{},keyPrefix:{}",metadata.cacheGroupKey,prefix);
					cacheProvider.clearGroup(metadata.cacheGroupKey,prefix);
				}
			}
		});
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<String> buildBatchUpdateIdCacheKeys(String mapperClassName,Object parameter){
		MapperMetadata entityInfo = MybatisMapperParser.getMapperMetadata(mapperClassName);
		Map map = (Map) parameter;
		List<Object> ids;
		if(map.containsKey("arg0")) {
			ids = (List<Object>) map.get("arg0");
		}else {
			ids = (List<Object>) map.get("param1");
		}
		
		List<String> keys = new ArrayList<>(ids.size());
		StringBuilder keyBuilder = new StringBuilder(entityInfo.getEntityClass().getSimpleName()).append(".id:");
		int prefixLen = keyBuilder.length();
		for (Object id : ids) {
			keyBuilder.append(id);
			keys.add(keyBuilder.toString());
			keyBuilder.setLength(prefixLen);
		}
		
		return keys;
	}
	
	/**
	 * 生成查询缓存key
	 * @param cacheInfo
	 * @param param
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String genarateQueryCacheKey(OnceContextVal invocationVal,String keyPattern,Object param){
		String text;
		try {
			Object[] args;
			if(param instanceof Map){
				Map<String, Object> map = (Map<String, Object>) param;
				if(map.containsKey(STR_PARAM + "1")){
					args = new String[map.size()/2];
					for (int i = 0; i < args.length; i++) {
						args[i] = CacheKeyUtils.objcetToString(map.get(STR_PARAM + (i+1)));
					}
				}else{
					args = new String[]{CacheKeyUtils.objcetToString(map)};
				}
			}else if(param instanceof BaseEntity){
				Serializable id = ((BaseEntity)param).getId();
				if(id != null && !"0".equals(id.toString())){	
					args = new String[]{(((BaseEntity)param).getId()).toString()};
				}else{
					args = new String[]{CacheKeyUtils.objcetToString(param)};
				}
			}else if(param instanceof Object[]){
				args = (Object[])param;
			}else if(param == null){
				args = new Object[0];
			}else{
				args = new String[]{CacheKeyUtils.objcetToString(param)};
			}
			
			text = StringUtils.join(args,"-");
		} catch (Exception e) {
			text = JsonUtils.toJson(param);
			e.printStackTrace();
		}
		if(text.length() > 64)text = DigestUtils.md5(text);

        String key = String.format(keyPattern, text);
		//按主键查询不区分
        QueryCacheMethodMetadata methodMetadata = invocationVal.getQueryMethodMetadata();
        if(methodMetadata == null || methodMetadata.isPk || methodMetadata.uniqueIndex) {
        	return key;
        }
        
        //考虑多租户已经数据权限，需要按租户及用户隔离
		StringBuilder sb = new StringBuilder(key);
		String tenantId = CurrentRuntimeContext.getTenantId();
		if(tenantId != null) {
			sb.append(GlobalConstants.AT).append(tenantId);
		}
		
		AuthUser currentUser;
		if(invocationVal.isUsingDataPermission() && (currentUser = CurrentRuntimeContext.getCurrentUser()) != null) {
			sb.append(GlobalConstants.AT).append(currentUser.getId());
		}
		
		return sb.toString();
	}
	

	
	private QueryCacheMethodMetadata getQueryMethodCache(String mtId){
		String key1 = mtId.substring(0, mtId.lastIndexOf(OnceContextVal.DOT));
		if(queryCacheMethods.containsKey(key1)){
			return queryCacheMethods.get(key1).get(mtId);
		}
		return null;
	}
	
	private QueryCacheMethodMetadata getQueryByPkMethodCache(String mtId){
		mtId = mtId.substring(0, mtId.lastIndexOf(OnceContextVal.DOT));
		if(queryCacheMethods.containsKey(mtId)){
			return queryCacheMethods.get(mtId).get(mtId + "." + CrudMethods.selectByPrimaryKey.name());
		}
		return null;
	}

	/**
	 * 生成按主键查询缓存定义
	 * @param mapperClass
	 * @param entityClass
	 * @return
	 */
	private QueryCacheMethodMetadata generateQueryByPKMethod(Class<?> mapperClass,Class<?> entityClass){
		QueryCacheMethodMetadata methodCache = null;
		Field[] fields = FieldUtils.getAllFields(entityClass);
		//主键key前缀
		for (Field field : fields) {
			if(field.isAnnotationPresent(Id.class)){
				methodCache = new QueryCacheMethodMetadata();
				methodCache.isPk = true;
				methodCache.collectionResult = false;
				methodCache.keyPattern = entityClass.getSimpleName() + ".id:%s";
				methodCache.methodName = mapperClass.getName() + "." + CrudMethods.selectByPrimaryKey.name();
				methodCache.expire = defaultCacheExpire;
				methodCache.cacheGroupKey = entityClass.getSimpleName() + GROUPKEY_SUFFIX;
				//methodCache.groupRalated = false;
				break;
			}
		}
		
		groupKeys.add(methodCache.cacheGroupKey);
		
		return methodCache;
	}
	
	private void generateUpdateByPkCacheMethod(Class<?> mapperClass,Class<?> entityClass,String keyPatternForPK){
		String methodName = null;
	    methodName = mapperClass.getName() + "." + CrudMethods.insert.name();
	    updatePkCacheMethods.put(methodName, new UpdateByPkCacheMethodMetadata(methodName, keyPatternForPK));
	    methodName = mapperClass.getName() + "." + CrudMethods.insertSelective.name();
	    updatePkCacheMethods.put(methodName, new UpdateByPkCacheMethodMetadata(methodName, keyPatternForPK));
	   //
        methodName = mapperClass.getName() + "." + CrudMethods.updateByPrimaryKey.name();
        updatePkCacheMethods.put(methodName, new UpdateByPkCacheMethodMetadata(methodName, keyPatternForPK));
        methodName = mapperClass.getName() + "." + CrudMethods.updateByPrimaryKeySelective.name();
        updatePkCacheMethods.put(methodName, new UpdateByPkCacheMethodMetadata(methodName, keyPatternForPK));

		//按主键删除
		methodName = mapperClass.getName() + "." +  CrudMethods.deleteByPrimaryKey.name();
		updatePkCacheMethods.put(methodName, new UpdateByPkCacheMethodMetadata(methodName, keyPatternForPK));

	}
	
	/**
	 * 按查询方法生成缓存key前缀
	 * @param entityClassName
	 * @param method
	 * @return
	 */
	private QueryCacheMethodMetadata generateQueryMethodCacheByMethod(MapperMetadata entityInfo,MapperMethod mapperMethod){

		Method method = mapperMethod.getMethod();
		Cache cacheAnnotation = method.getAnnotation(Cache.class);
		String[] evictOnMethods = cacheAnnotation.evictOnMethods();
		Class<?> mapperClass = entityInfo.getMapperClass();
		Class<?> entityClass = entityInfo.getEntityClass();
		QueryCacheMethodMetadata methodCache = new QueryCacheMethodMetadata();
		methodCache.methodName = mapperClass.getName() + OnceContextVal.DOT + method.getName();
		methodCache.concurrency = cacheAnnotation.concurrency();
		methodCache.uniqueIndex = cacheAnnotation.uniqueIndex();
		methodCache.cacheGroupKey = entityClass.getSimpleName() + GROUPKEY_SUFFIX;
		if(cacheAnnotation.userScope()){
			methodCache.contextParam = CURRENT_USER_CONTEXT_NAME;
		}else if(cacheAnnotation.scopeContext().length > 0){
			methodCache.contextParam = cacheAnnotation.scopeContext()[0];
		}
		
		if(cacheAnnotation.refKey().length > 0){
			methodCache.refKey = cacheAnnotation.refKey()[0];
		}
		
		if(methodCache.contextParam != null && evictOnMethods.length == 0){
			evictOnMethods = new String[]{"*"};
		}
		
		methodCache.checkExpired = evictOnMethods.length > 0;
		if(cacheAnnotation.expire() > 0){
			methodCache.expire = cacheAnnotation.expire();
		}else if(cacheAnnotation.userScope()){
			methodCache.expire = IN_1MINS * 10 < defaultCacheExpire ? IN_1MINS * 10 : defaultCacheExpire;
		}
		
		if(methodCache.uniqueIndex && method.getReturnType() != entityClass){
			throw new MybatisHanlerInitException("@Cache with[uniqueIndex = true] but ReturnType not Match ["+entityClass.getName()+"]");
		}
		methodCache.collectionResult = method.getReturnType() == List.class || method.getReturnType() == Set.class;
		methodCache.groupRalated = methodCache.collectionResult || !methodCache.uniqueIndex;
		
		methodCache.fieldNames = new String[method.getParameterTypes().length];
		Annotation[][] annotations = method.getParameterAnnotations();
		boolean uniqueQuery = method.getReturnType().isAnnotationPresent(Table.class);
		for (int i = 0; i < annotations.length; i++) {
			Annotation[] aa = annotations[i];
			if(aa.length > 0){
				String fieldName = null;
				inner:for (Annotation annotation : aa) {
					if(annotation.toString().contains(Param.class.getName())){
						fieldName = ((Param)annotation).value();
						break inner;
					}
				}
				if(uniqueQuery && entityInfo.getPropToColumnMappings().containsKey(fieldName)){					
					methodCache.fieldNames[i] = fieldName;
				}
			}
			//
		}
		methodCache.keyPattern = new StringBuilder(entityClass.getSimpleName()).append(OnceContextVal.DOT).append(method.getName()).append(":%s").toString();
		
		if(uniqueQuery){
			for (String name : methodCache.fieldNames) {
				if(StringUtils.isBlank(name)){
					methodCache.fieldNames = null;
					break;
				}
			}
		}
		//
		buildEvictOnMethods(mapperClass.getName(),mapperMethod,evictOnMethods);

		return methodCache;
	}
	
	/**
	 * 构建自定义缓存更新关系
	 * @param mapperClassName
	 * @param method
	 * @param evictOnMethods
	 */
	private void buildEvictOnMethods(String mapperClassName,MapperMethod method,String[] evictOnMethods) {
		if(evictOnMethods == null|| evictOnMethods.length == 0){
			return;
		}
		
		String targetMethodFullNamePrefix = mapperClassName.substring(0, mapperClassName.lastIndexOf(".") + 1);
		String targetMapperClassName = null;
		for (String methodName : evictOnMethods) {
			if("*".equals(methodName)){
				methodName = mapperClassName + ".*";
			}else if(!methodName.contains(OnceContextVal.DOT)) {
				methodName = mapperClassName + OnceContextVal.DOT + methodName;
			}
			if(!methodName.startsWith(targetMethodFullNamePrefix)){
				methodName = targetMethodFullNamePrefix + methodName;
			}
			targetMapperClassName = methodName.substring(0,methodName.lastIndexOf("."));
			if(!methodName.endsWith("*")){
				addCacheCheckRelations(methodName, method.getFullName());
			}else{
				MapperMetadata methodEntityInfo = MybatisMapperParser.getMapperMetadata(targetMapperClassName);
				if(methodEntityInfo == null){
					continue;
				}

				for (MapperMethod mm : methodEntityInfo.getMapperMethods().values()) {
					if(mm.getSqlType() == SqlCommandType.SELECT)continue;
					if(mm.getFullName().contains(methodName.replace("*", ""))){
						addCacheCheckRelations(mm.getFullName(), method.getFullName());
					}
				}
			}
		}
		
	}
	
	
	private void addCacheCheckRelations(String updateMethodName,String queryMethodName){
		 List<String> list = customUpdateCacheMapppings.get(updateMethodName);
		 if(list == null){
			 list = new ArrayList<>();
			 customUpdateCacheMapppings.put(updateMethodName, list);
		 }
		 list.add(queryMethodName);
	}
	
	@Override
	public void close() {
       cleanCacheExecutor.shutdown();		
	}

	@Override
	public int interceptorOrder() {
		return 1;
	}
}
