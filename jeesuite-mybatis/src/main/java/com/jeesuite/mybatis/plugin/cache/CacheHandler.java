package com.jeesuite.mybatis.plugin.cache;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.concurrent.RetryAsyncTaskExecutor;
import com.jeesuite.common.concurrent.RetryTask;
import com.jeesuite.common.concurrent.StandardThreadExecutor;
import com.jeesuite.common.concurrent.StandardThreadExecutor.StandardThreadFactory;
import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.common.util.DigestUtils;
import com.jeesuite.common.util.ReflectUtils;
import com.jeesuite.mybatis.MybatisConfigs;
import com.jeesuite.mybatis.MybatisRuntimeContext;
import com.jeesuite.mybatis.core.BaseEntity;
import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.crud.CrudMethods;
import com.jeesuite.mybatis.exception.MybatisHanlerInitException;
import com.jeesuite.mybatis.kit.CacheKeyUtils;
import com.jeesuite.mybatis.kit.MybatisSqlRewriteUtils.SqlMetadata;
import com.jeesuite.mybatis.parser.EntityInfo;
import com.jeesuite.mybatis.parser.EntityInfo.MapperMethod;
import com.jeesuite.mybatis.parser.MybatisMapperParser;
import com.jeesuite.mybatis.plugin.JeesuiteMybatisInterceptor;
import com.jeesuite.mybatis.plugin.cache.annotation.Cache;
import com.jeesuite.mybatis.plugin.cache.annotation.CacheIgnore;
import com.jeesuite.mybatis.plugin.cache.provider.DefaultCacheProvider;
import com.jeesuite.mybatis.plugin.pagination.PageExecutor;
import com.jeesuite.spring.InstanceFactory;


/**
 * 自动缓存拦截处理
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 * @Copyright (c) 2015, jwww
 */
public class CacheHandler implements InterceptorHandler {


	protected static final Logger logger = LoggerFactory.getLogger("com.jeesuite.mybatis.plugin.cache");

	public static final String CURRENT_USER_CONTEXT_NAME = "currentUser";
	private static final String BLOCK_ON_CONCURRENT_LOCK_RETURN = "_block_on_concurrentLock";
	public static final String NAME = "cache";
	public final static long IN_1MINS = 60;
    public final static long IN_1HOUR = 60 * 60;
	public static long defaultCacheExpire = 0;
	
	private static final String STR_PARAM = "param";
	protected static final String DOT = ".";
	
	public static final String GROUPKEY_SUFFIX = "~keys";
	private boolean dynamicCacheTime = false;
	private boolean nullValueCache = false;
	//null缓存占位符（避免频繁查询不存在对象造成缓存穿透导致频繁查询db）
	public static final String NULL_PLACEHOLDER = "~null";
	
	private static List<String> groupKeys = new ArrayList<>();
	
	//需要缓存的所有mapper
	private static List<String> cacheEnableMappers = new ArrayList<>();
	
	private static Map<String, Map<String, QueryCacheMethodMetadata>> queryCacheMethods = new HashMap<>();
	
	private static Map<String, UpdateByPkCacheMethodMetadata> updatePkCacheMethods = new HashMap<>();
	
	//<更新方法msId,[关联查询方法列表]>
	private static Map<String, List<String>> customUpdateCacheMapppings = new HashMap<>();

	//记录当前线程写入的所有缓存key
	private static ThreadLocal<List<String>>  transactionWriteCacheKeys = new ThreadLocal<>();
	
	private ThreadLocal<String>  concurrentLockKey = new ThreadLocal<>();
	
	protected static CacheProvider cacheProvider;
	
	public void setCacheProvider(CacheProvider cacheProvider) {
		CacheHandler.cacheProvider = cacheProvider;
	}

	private static CacheProvider getCacheProvider() {
		if(cacheProvider == null){
			synchronized (CacheHandler.class) {
				if(cacheProvider != null)return cacheProvider;
				if(cacheProvider == null){
					cacheProvider = InstanceFactory.getInstance(CacheProvider.class);
				}
				if(cacheProvider == null){					
					cacheProvider = new DefaultCacheProvider();
				}
				logger.info("Initializing cacheProvider use:{} ",cacheProvider.getClass().getName());
			}
		}
		return cacheProvider;
	}
	
	@Override
	public void start(JeesuiteMybatisInterceptor context) {
		
		nullValueCache = MybatisConfigs.getBoolean(context.getGroupName(), MybatisConfigs.CACHE_NULL_VALUE, false);
		dynamicCacheTime = MybatisConfigs.getBoolean(context.getGroupName(), MybatisConfigs.CACHE_DYNAMIC_EXPIRE, false);
		defaultCacheExpire = Long.parseLong(MybatisConfigs.getProperty(context.getGroupName(), MybatisConfigs.CACHE_EXPIRE_SECONDS, "0"));
		logger.info("nullValueCache:{},defaultCacheExpireSeconds:{},dynamicCacheTime:{}",nullValueCache,defaultCacheExpire,dynamicCacheTime);

		List<EntityInfo> entityInfos = MybatisMapperParser.getEntityInfos(context.getGroupName());
		
		Class<BaseEntity> baseEntityClass = BaseEntity.class;
		QueryCacheMethodMetadata methodCache = null;
		for (EntityInfo ei : entityInfos) {
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
					logger.info("解析查询方法{}自动缓存配置 ok,keyPattern:[{}]",methodCache.methodName,methodCache.keyPattern);
				}
			}
			//缓存需要自动缓存的mapper
			cacheEnableMappers.add(ei.getMapperClass().getName());
			logger.info("解析查询方法{}自动缓存配置 ok,keyPattern:[{}]",queryByPKMethod.methodName,queryByPKMethod.keyPattern);
			
			queryCacheMethods.put(mapperClass.getName(), tmpMap);
			
			//更新缓存方法
			generateUpdateByPkCacheMethod(mapperClass, ei.getEntityClass(), keyPatternForPK);
		}
		//
		logger.info(">>>customUpdateCacheMapppings:{}",customUpdateCacheMapppings);
	}

	@Override
	public Object onInterceptor(Invocation invocation) throws Throwable {

		Object[] args = invocation.getArgs();
		MappedStatement mt = (MappedStatement)args[0]; 

		concurrentLockKey.remove();
		
		boolean getLock = false;
		String cacheKey = null;
		if(mt.getSqlCommandType().equals(SqlCommandType.SELECT)){	
			//分页查询
			if(PageExecutor.getPageParams() != null && PageExecutor.getPageParams().getPageNo() > 1){
				return null;
			}
			//事务方法内部的查询不走缓存
			if(MybatisRuntimeContext.isTransactionalOn()){
				if(logger.isDebugEnabled())logger.debug(">>auto_cache_process skipCache[isTransactionalOn] -> mapperId:{}",mt.getId());
				return null;
			}
			//按主键查询
			QueryCacheMethodMetadata metadata = getQueryMethodCache(mt.getId());
			if(metadata == null)return null;

			cacheKey = genarateQueryCacheKey(metadata.keyPattern, args[1]);
			//并发控制防止缓存穿透
			if(!metadata.concurrency){
				concurrentLockKey.set("concurrent:" + cacheKey);
				getLock = getCacheProvider().setnx(concurrentLockKey.get(), "1", 30);
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
				cacheObject = getCacheProvider().get(cacheKey);
				nullPlaceholder = nullValueCache && NULL_PLACEHOLDER.equals(cacheObject);
				if(StringUtils.isNotBlank(metadata.refKey) && (nullPlaceholder || cacheObject == null)){
					cacheObject = getCacheProvider().get(metadata.refKey);
					nullPlaceholder = nullValueCache && NULL_PLACEHOLDER.equals(cacheObject);
				}
				if(nullPlaceholder){
					if(logger.isDebugEnabled())logger.debug(">>auto_cache_process hitCache -> mapperId:{},cacheKey:{}",mt.getId(),cacheKey);
				}else if(cacheObject != null){
					if(logger.isDebugEnabled())logger.debug(">>auto_cache_process hitCache -> mapperId:{},cacheKey:{}",mt.getId(),cacheKey);
				}
			}else{
				//新根据缓存KEY找到与按ID缓存的KEY
				String refCacheKey = nullValueCache ? getCacheProvider().get(cacheKey) : getCacheProvider().getStr(cacheKey);
				if(refCacheKey != null){
					if(nullPlaceholder = (nullValueCache && NULL_PLACEHOLDER.equals(refCacheKey))){
						cacheObject = NULL_PLACEHOLDER;
					}else{						
						cacheObject = getCacheProvider().get(refCacheKey);
						if(cacheObject != null && logger.isDebugEnabled())logger.debug(">>auto_cache_process  hitRefCache -> mapperId:{},cacheKey:{},refCacheKey:{}",mt.getId(),cacheKey,refCacheKey);
					}
				}
			}
			
			if(nullPlaceholder){
				cacheObject = new ArrayList<>(0);
			}else if(cacheObject != null && !(cacheObject instanceof Collection)){						
				cacheObject = Arrays.asList(cacheObject);
			}
			
			return cacheObject;
		} 
		
		return null;
	
	}
	

	@SuppressWarnings("rawtypes")
	@Override
	public void onFinished(Invocation invocation,Object result) {
		try {
			if(BLOCK_ON_CONCURRENT_LOCK_RETURN.equals(result))return;
			Object[] args = invocation.getArgs();
			MappedStatement mt = (MappedStatement)args[0]; 
			
			String mapperClassName = mt.getId().substring(0, mt.getId().lastIndexOf(DOT));
			
			QueryCacheMethodMetadata metadata = null;
			if(mt.getSqlCommandType().equals(SqlCommandType.SELECT)){	
				if(result == null)return; 
				if((metadata = getQueryMethodCache(mt.getId())) == null)return;
				
				final String cacheKey = genarateQueryCacheKey(metadata.keyPattern, args[1]);
				if(result instanceof List){
					List list = (List)result;
					if(list.isEmpty()){
						if(nullValueCache){
							getCacheProvider().set(cacheKey,NULL_PLACEHOLDER, IN_1MINS);
						}
						return;
					}
					result = metadata.collectionResult ? result : list.get(0);
				}
				//
				if(!metadata.isSecondQueryById()){
					if(getCacheProvider().set(cacheKey,result, metadata.getExpire())){
						if(logger.isDebugEnabled())logger.debug(">>auto_cache_process addCache -> mapperId:{},cacheKey:{}",mt.getId(),cacheKey);
					}
					
					if(metadata.isPk){//唯一索引（业务上）
						cacheUniqueSelectRef(result, mt, cacheKey);
					}else if(metadata.groupRalated){//结果为集合的情况，增加key到cacheGroup
						getCacheProvider().putGroup(metadata.cacheGroupKey, cacheKey);
					}
				}else{
					//之前没有按主键的缓存，增加按主键缓存
					String idCacheKey = genarateQueryCacheKey(getQueryByPkMethodCache(mt.getId()).keyPattern,result);
					
					if(idCacheKey != null && cacheKey != null){
						if(!getCacheProvider().exists(idCacheKey)){						
							//缓存idkey->实体
							getCacheProvider().set(idCacheKey,result, metadata.getExpire());
						}
						//缓存fieldkey->idkey
						cacheFieldRefKey(cacheKey,idCacheKey, metadata.getExpire());
						if(logger.isDebugEnabled())logger.debug(">>auto_cache_process addCache -> mapperId:{},idCacheKey:{},cacheKey:{}",mt.getId(),idCacheKey,cacheKey);
					}
				}
			}else{
				if(!cacheEnableMappers.contains(mapperClassName))return;
				//返回0，未更新成功
				if(result != null && ((int)result) == 0)return;
				
				boolean insertAction = mt.getSqlCommandType().equals(SqlCommandType.INSERT);
				boolean updateAction = mt.getSqlCommandType().equals(SqlCommandType.UPDATE);
				boolean deleteAcrion = mt.getSqlCommandType().equals(SqlCommandType.DELETE);
				
				if(updatePkCacheMethods.containsKey(mt.getId())){
					String idCacheKey = null;
					UpdateByPkCacheMethodMetadata updateMethodCache = updatePkCacheMethods.get(mt.getId());
					if(deleteAcrion){
						idCacheKey = genarateQueryCacheKey(updateMethodCache.keyPattern,args[1]);
						getCacheProvider().remove(idCacheKey);
						if(logger.isDebugEnabled())logger.debug(">>auto_cache_process removeCache -> mapperId:{},idCacheKey:{}",mt.getId(),idCacheKey);
					}else{
						idCacheKey = genarateQueryCacheKey(updateMethodCache.keyPattern,args[1]);
						if(insertAction || updateAction){
							if(result != null){
								QueryCacheMethodMetadata queryByPkMethodCache = getQueryByPkMethodCache(mt.getId());
								getCacheProvider().set(idCacheKey,args[1], queryByPkMethodCache.getExpire());
								//插入其他唯一字段引用
								if(insertAction)cacheUniqueSelectRef(args[1], mt, idCacheKey);
								if(logger.isDebugEnabled())logger.debug(">>auto_cache_process addCache -> mapperId:{},idCacheKey:{}",mt.getId(),idCacheKey);
								//
								addCurrentThreadCacheKey(idCacheKey);
							}
						}
					}	
				}else {
					/**EntityInfo entityInfo = MybatisMapperParser.getEntityInfoByMapper(mapperClassName);
					//非按主键更新方法，按查询条件查询相关内容，然后清理缓存
					final Object parameter = args[1];
					BoundSql boundSql = mt.getBoundSql(parameter);
					String orignSql = StringUtils.replace(boundSql.getSql(), ";$", StringUtils.EMPTY);
					SqlMetadata selectIdsSql = MybatisSqlRewriteUtils.rewriteAsSelectPkField(orignSql, entityInfo.getIdColumn());
					//
					removeCacheByDyncQuery(selectIdsSql,parameter);
					*/
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
			String lockKey = concurrentLockKey.get();
			if(lockKey != null){
				cacheProvider.remove(lockKey);
				concurrentLockKey.remove();
			}
		}
	}

	/**
	 * 缓存其他唯一结果查询方法和主键缓存的引用
	 * @param object
	 * @param mt
	 * @param cacheKey
	 */
	private void cacheUniqueSelectRef(Object object, MappedStatement mt, String cacheKey) {
		Collection<QueryCacheMethodMetadata> mcs = queryCacheMethods.get(mt.getId().substring(0, mt.getId().lastIndexOf(DOT))).values();
		outter:for (QueryCacheMethodMetadata methodCache : mcs) {
			if(!methodCache.isSecondQueryById())continue;
			try {	
				Object[] cacheFieldValues = new Object[methodCache.fieldNames.length];
				for (int i = 0; i < cacheFieldValues.length; i++) {
					if(methodCache.fieldNames[i] == null)break outter;
					cacheFieldValues[i] = ReflectUtils.getObjectValue(object, methodCache.fieldNames[i]);
					if(cacheFieldValues[i] == null)continue outter;
				}
				String fieldCacheKey = genarateQueryCacheKey(methodCache.keyPattern , cacheFieldValues);
				
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
			getCacheProvider().set(fieldCacheKey, idCacheKey, expired);
		}else{
			getCacheProvider().setStr(fieldCacheKey, idCacheKey, expired);
		}
	}
	
	/**
	 * 根据动态查询内容清理缓存
	 * @param selectIdsSql 查询主键列表SQL语句信息
	 * @param parameter 参数
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void removeCacheByDyncQuery(SqlMetadata selectIdsSql, Object parameter) {
		List<Object> parameterList = new ArrayList<>(selectIdsSql.getParameterNums());
		if(parameter instanceof Map){
			Map<String, Object> map = (Map<String, Object>) parameter;
			Object value;
			for (int i = selectIdsSql.getWhereParameterIndex(); i < 100; i++) {
				value = map.get(STR_PARAM + i);
				if(value == null){
					break;
				}
				
			}
		}else if(parameter instanceof Collection) {
			((Collection) parameter).addAll((Collection) parameter);
		}else {
			parameterList.add(parameter);
		}
		
	}
	
	/**
	 * 删除缓存组
	 * @param msId
	 * @param mapperClassName
	 * @param removePkCache  是否同时删除按主键的缓存
	 */
	private void removeCacheByGroup(String msId, String mapperClassName) {
		EntityInfo entityInfo = MybatisMapperParser.getEntityInfoByMapper(mapperClassName);
		if(entityInfo == null)return;
		final String groupName = entityInfo.getEntityClass().getSimpleName();
		
		RetryAsyncTaskExecutor.execute(new RetryTask() {
			@Override
			public String traceId() {
				return msId;
			}
			
			@Override
			public boolean process() throws Exception {
				getCacheProvider().clearGroup(groupName);
				if(logger.isDebugEnabled())logger.debug(">>auto_cache_process removeGroupCache -> mapperId:{},groupName:{}",msId,groupName);
				return true;
			}
		});
	}
	
	/**
	 * 删除更新方法自定义缓存关系
	 * @param updateId
	 */
	private void removeCustomRelateCache(String updateId) {
		final List<String> queryMethods = customUpdateCacheMapppings.get(updateId);
		RetryAsyncTaskExecutor.execute(new RetryTask() {
			@Override
			public String traceId() {
				return updateId;
			}
			@Override
			public boolean process() throws Exception {
				QueryCacheMethodMetadata metadata;
				for (String method : queryMethods) {
					metadata = getQueryMethodCache(method);
					String prefix = StringUtils.splitByWholeSeparator(metadata.keyPattern, "%s")[0];
					cacheProvider.clearGroup(metadata.cacheGroupKey,prefix);
				}
				return true;
			}
		});
	}
	
	/**
	 * 生成查询缓存key
	 * @param cacheInfo
	 * @param param
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String genarateQueryCacheKey(String keyPattern,Object param){
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

		return String.format(keyPattern, text);
	}
	

	
	private QueryCacheMethodMetadata getQueryMethodCache(String mtId){
		String key1 = mtId.substring(0, mtId.lastIndexOf(DOT));
		if(queryCacheMethods.containsKey(key1)){
			return queryCacheMethods.get(key1).get(mtId);
		}
		return null;
	}
	
	private QueryCacheMethodMetadata getQueryByPkMethodCache(String mtId){
		mtId = mtId.substring(0, mtId.lastIndexOf(DOT));
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
		Field[] fields = entityClass.getDeclaredFields();
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
				break;
			}
		}
		
		groupKeys.add(methodCache.cacheGroupKey);
		
		return methodCache;
	}
	
	private void generateUpdateByPkCacheMethod(Class<?> mapperClass,Class<?> entityClass,String keyPatternForPK){
		String methodName = null;
	    methodName = mapperClass.getName() + "." + CrudMethods.insert.name();
	    updatePkCacheMethods.put(methodName, new UpdateByPkCacheMethodMetadata(entityClass,methodName, keyPatternForPK, SqlCommandType.INSERT));
	    methodName = mapperClass.getName() + "." + CrudMethods.insertSelective.name();
	    updatePkCacheMethods.put(methodName, new UpdateByPkCacheMethodMetadata(entityClass,methodName, keyPatternForPK, SqlCommandType.INSERT));
	   //
        methodName = mapperClass.getName() + "." + CrudMethods.updateByPrimaryKey.name();
        updatePkCacheMethods.put(methodName, new UpdateByPkCacheMethodMetadata(entityClass,methodName, keyPatternForPK, SqlCommandType.UPDATE));
        methodName = mapperClass.getName() + "." + CrudMethods.updateByPrimaryKeySelective.name();
        updatePkCacheMethods.put(methodName, new UpdateByPkCacheMethodMetadata(entityClass,methodName, keyPatternForPK, SqlCommandType.UPDATE));

		//按主键删除
		methodName = mapperClass.getName() + "." +  CrudMethods.deleteByPrimaryKey.name();
		updatePkCacheMethods.put(methodName, new UpdateByPkCacheMethodMetadata(entityClass,methodName, keyPatternForPK, SqlCommandType.DELETE));

	}
	
	/**
	 * 按查询方法生成缓存key前缀
	 * @param entityClassName
	 * @param method
	 * @return
	 */
	private QueryCacheMethodMetadata generateQueryMethodCacheByMethod(EntityInfo entityInfo,MapperMethod mapperMethod){

		Method method = mapperMethod.getMethod();
		Cache cacheAnnotation = method.getAnnotation(Cache.class);
		String[] evictOnMethods = cacheAnnotation.evictOnMethods();
		Class<?> mapperClass = entityInfo.getMapperClass();
		Class<?> entityClass = entityInfo.getEntityClass();
		QueryCacheMethodMetadata methodCache = new QueryCacheMethodMetadata();
		methodCache.methodName = mapperClass.getName() + DOT + method.getName();
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
		if(methodCache.collectionResult){			
			methodCache.groupRalated = true;
		}else{
			// count等统计查询
			methodCache.groupRalated = method.getReturnType().isAnnotationPresent(Table.class) == false;
		}
		
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
				if(uniqueQuery && MybatisMapperParser.entityHasProperty(entityClass, fieldName)){					
					methodCache.fieldNames[i] = fieldName;
				}
			}
			//
		}
		methodCache.keyPattern = new StringBuilder(entityClass.getSimpleName()).append(DOT).append(method.getName()).append(":%s").toString();
		
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
			}
			if(!methodName.startsWith(targetMethodFullNamePrefix)){
				methodName = targetMethodFullNamePrefix + methodName;
			}
			targetMapperClassName = methodName.substring(0,methodName.lastIndexOf("."));
			if(!methodName.endsWith("*")){
				addCacheCheckRelations(methodName, method.getFullName());
			}else{
				EntityInfo methodEntityInfo = MybatisMapperParser.getEntityInfoByMapper(targetMapperClassName);
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
		 //放入cache方法列表，避免跳过
		 String mapperId = updateMethodName.substring(0, updateMethodName.lastIndexOf(DOT));
		 if(!cacheEnableMappers.contains(mapperId)) {
			 cacheEnableMappers.add(mapperId);
		 }
	}
	
	private void addCurrentThreadCacheKey(String key){
		List<String> keys =  transactionWriteCacheKeys.get();
		if(keys == null){
			keys = new ArrayList<>();
			 transactionWriteCacheKeys.set(keys);
		}
		keys.add(key);
	}
	
	/**
	 * 回滚当前事务写入的缓存
	 */
	public static void rollbackCache(){
		List<String> keys =  transactionWriteCacheKeys.get();
		if(keys == null)return;
		for (String key : keys) {
			getCacheProvider().remove(key);
		}
	}

	@Override
	public void close() {
		try {			
			getCacheProvider().close();
		} catch (Exception e) {}
	}

	@Override
	public int interceptorOrder() {
		return 1;
	}
}
