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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.common.util.DigestUtils;
import com.jeesuite.mybatis.MybatisConfigs;
import com.jeesuite.mybatis.MybatisRuntimeContext;
import com.jeesuite.mybatis.core.BaseEntity;
import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.crud.CrudMethodDefine;
import com.jeesuite.mybatis.crud.name.DefaultCrudMethodDefine;
import com.jeesuite.mybatis.crud.name.Mapper3CrudMethodDefine;
import com.jeesuite.mybatis.exception.MybatisHanlerInitException;
import com.jeesuite.mybatis.kit.CacheKeyUtils;
import com.jeesuite.mybatis.kit.ReflectUtils;
import com.jeesuite.mybatis.parser.EntityInfo;
import com.jeesuite.mybatis.parser.EntityInfo.MapperMethod;
import com.jeesuite.mybatis.parser.MybatisMapperParser;
import com.jeesuite.mybatis.plugin.JeesuiteMybatisInterceptor;
import com.jeesuite.mybatis.plugin.cache.annotation.Cache;
import com.jeesuite.mybatis.plugin.cache.provider.DefaultCacheProvider;
import com.jeesuite.spring.InstanceFactory;


/**
 * 自动缓存拦截处理
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 * @Copyright (c) 2015, jwww
 */
public class CacheHandler implements InterceptorHandler {


	protected static final Logger logger = LoggerFactory.getLogger(CacheHandler.class);

	public static final String CURRENT_USER_CONTEXT_NAME = "currentUser";
	private static final String BLOCK_ON_CONCURRENT_LOCK_RETURN = "_block_on_concurrentLock";
	public static final String NAME = "cache";
	public final static long IN_1MINS = 60;
    public final static long IN_1HOUR = 60 * 60;
	public static long defaultCacheExpire = IN_1HOUR;
	
	private static final String STR_PARAM = "param";
	protected static final String DOT = ".";
	// 计算关联key集合权重的基数
	private static long baseScore = System.currentTimeMillis() / 1000 - 3600;
	
	private boolean dynamicCacheTime = false;
	private boolean nullValueCache = false;
	//null缓存占位符（避免频繁查询不存在对象造成缓存穿透导致频繁查询db）
	public static final String NULL_PLACEHOLDER = "~null";
	private static final String VALUE_TEMPLATE = "_%s_%s";
	private static final String KEY_NAME = "cache.expired.methods";
	
	//需要缓存的所有mapper
	private static List<String> cacheEnableMappers = new ArrayList<>();
	
	private static Map<String, Map<String, QueryMethodCache>> queryCacheMethods = new HashMap<>();
	
	private static Map<String, UpdateByPkMethodCache> updateCacheMethods = new HashMap<>();

	//记录当前线程写入的所有缓存key
	private static ThreadLocal<List<String>>  transactionWriteCacheKeys = new ThreadLocal<>();
	
	private ThreadLocal<String>  concurrentLockKey = new ThreadLocal<>();
	
	//<mtId,[关联查询方法列表]>
	private static Map<String, List<String>> requiredCheckCacheMethodMapppings = new HashMap<>();
	
	protected static CacheProvider cacheProvider;
	
	private CrudMethodDefine methodDefine;
	
	private ScheduledExecutorService clearExpiredKeysTimer;
	
	public void setCacheProvider(CacheProvider cacheProvider) {
		CacheHandler.cacheProvider = cacheProvider;
	}

	private static CacheProvider getCacheProvider() {
		if(cacheProvider == null){
			synchronized (CacheHandler.class) {
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
	public Object onInterceptor(Invocation invocation) throws Throwable {

		Object[] args = invocation.getArgs();
		MappedStatement mt = (MappedStatement)args[0]; 

		concurrentLockKey.remove();
		
		boolean getLock = false;
		String cacheKey = null;
		if(mt.getSqlCommandType().equals(SqlCommandType.SELECT)){	
			//事务方法内部的查询不走缓存
			if(MybatisRuntimeContext.isTransactionalOn()){
				if(logger.isDebugEnabled())logger.debug(">>auto_cache_process  isTransactionalOn SKIP -> mapperId:{}",mt.getId());
				return null;
			}
			//按主键查询
			QueryMethodCache cacheInfo = getQueryMethodCache(mt.getId());
			if(cacheInfo == null)return null;
			//
			if(skipCache(cacheInfo)){
				if(logger.isDebugEnabled())logger.debug(">>auto_cache_process cache_mark_expired SKIP -> userId:{}, mapperId:{}",MybatisRuntimeContext.getCurrentUserId(),mt.getId());
				return null;
			}
			cacheKey = genarateQueryCacheKey(cacheInfo.keyPattern, args[1]);
			//并发控制防止缓存穿透
			if(!cacheInfo.concurrency){
				concurrentLockKey.set("concurrent:" + cacheKey);
				getLock = getCacheProvider().setnx(concurrentLockKey.get(), "1", 30);
				if(!getLock){
					if(logger.isDebugEnabled())logger.debug(">>auto_cache_process not_getConcurrentLock BLOCK -> mapperId:{}",mt.getId());
					return BLOCK_ON_CONCURRENT_LOCK_RETURN;
				}
				if(logger.isDebugEnabled())logger.debug(">>auto_cache_process getConcurrentLock CONTINUE -> mapperId:{}",mt.getId());
			}
			
			Object cacheObject = null;
			boolean nullPlaceholder = false;
			//
			if(!cacheInfo.isSecondQueryById()){
				//从缓存读取
				cacheObject = getCacheProvider().get(cacheKey);
				nullPlaceholder = nullValueCache && NULL_PLACEHOLDER.equals(cacheObject);
				if(StringUtils.isNotBlank(cacheInfo.refKey) && (nullPlaceholder || cacheObject == null)){
					cacheObject = getCacheProvider().get(cacheInfo.refKey);
					nullPlaceholder = nullValueCache && NULL_PLACEHOLDER.equals(cacheObject);
				}
				if(nullPlaceholder){
					logger.debug(">>auto_cache_process method[{}] find NULL_PLACEHOLDER result from cacheKey:{}",mt.getId(),cacheKey);
				}else if(cacheObject != null){
					logger.debug(">>auto_cache_process method[{}] find result from cacheKey:{}",mt.getId(),cacheKey);
				}
			}else{
				//新根据缓存KEY找到与按ID缓存的KEY
				String refCacheKey = nullValueCache ? getCacheProvider().get(cacheKey) : getCacheProvider().getStr(cacheKey);
				if(refCacheKey != null){
					if(nullPlaceholder = (nullValueCache && NULL_PLACEHOLDER.equals(refCacheKey))){
						cacheObject = NULL_PLACEHOLDER;
					}else{						
						cacheObject = getCacheProvider().get(refCacheKey);
						if(cacheObject != null && logger.isDebugEnabled())logger.debug(">>auto_cache_process method[{}] find result from cacheKey:{} ,ref by:{}",mt.getId(),refCacheKey,cacheKey);
					}
				}
			}
			
			if(nullPlaceholder){
				cacheObject = new ArrayList<>();
			}else if(cacheObject != null && !(cacheObject instanceof Collection)){						
				cacheObject = new ArrayList<>(Arrays.asList(cacheObject));
			}
			
			return cacheObject;
		} else{
			tryRemarkCleanRalationCache(mt);
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
			
			String mapperNameSpace = mt.getId().substring(0, mt.getId().lastIndexOf(DOT));
			
			QueryMethodCache cacheInfo = null;
			if(mt.getSqlCommandType().equals(SqlCommandType.SELECT)){	
				if(result == null)return; 
				if((cacheInfo = getQueryMethodCache(mt.getId())) == null)return;
				
				final String cacheKey = genarateQueryCacheKey(cacheInfo.keyPattern, args[1]);
				if(result instanceof List){
					List list = (List)result;
					if(list.isEmpty()){
						if(nullValueCache){
							getCacheProvider().set(cacheKey,NULL_PLACEHOLDER, IN_1MINS);
						}
						return;
					}
					result = cacheInfo.collectionResult ? result : list.get(0);
				}
				//
				if(!cacheInfo.isSecondQueryById()){
					if(getCacheProvider().set(cacheKey,result, cacheInfo.getExpire())){
						if(logger.isDebugEnabled())logger.debug(">>auto_cache_process method[{}] put result to cache，cacheKey:{}",mt.getId(),cacheKey);
					}
					if(cacheInfo.uniqueIndex){
						cacheUniqueSelectRef(result, mt, cacheKey);
					}
				}else{
					//之前没有按主键的缓存，增加按主键缓存
					String idCacheKey = genarateQueryCacheKey(getQueryByPkMethodCache(mt.getId()).keyPattern,result);
					
					if(idCacheKey != null && cacheKey != null){
						if(!getCacheProvider().exists(idCacheKey)){						
							//缓存idkey->实体
							getCacheProvider().set(idCacheKey,result, cacheInfo.getExpire());
						}
						//缓存fieldkey->idkey
						cacheFieldRefKey(cacheKey,idCacheKey, cacheInfo.getExpire());
						if(logger.isDebugEnabled())logger.debug(">>auto_cache_process method[{}] put result to cache，cacheKey:{},and add ref cacheKey:{}",mt.getId(),idCacheKey,cacheKey);
					}
				}
			}else{
				if(!cacheEnableMappers.contains(mapperNameSpace))return;
				//返回0，未更新成功
				if(result != null && ((int)result) == 0)return;
				
				boolean insertAction = mt.getSqlCommandType().equals(SqlCommandType.INSERT);
				boolean updateAction = mt.getSqlCommandType().equals(SqlCommandType.UPDATE);
				boolean deleteAcrion = mt.getSqlCommandType().equals(SqlCommandType.DELETE);
				
				if(updateCacheMethods.containsKey(mt.getId())){
					String idCacheKey = null;
					UpdateByPkMethodCache updateMethodCache = updateCacheMethods.get(mt.getId());
					if(deleteAcrion){
						idCacheKey = genarateQueryCacheKey(updateMethodCache.keyPattern,args[1]);
						getCacheProvider().remove(idCacheKey);
						if(logger.isDebugEnabled())logger.debug(">>auto_cache_process method[{}] remove cacheKey:{} from cache",mt.getId(),idCacheKey);
					}else{
						idCacheKey = genarateQueryCacheKey(updateMethodCache.keyPattern,args[1]);
						if(insertAction || updateAction){
							if(result != null){
								QueryMethodCache queryByPkMethodCache = getQueryByPkMethodCache(mt.getId());
								getCacheProvider().set(idCacheKey,args[1], queryByPkMethodCache.getExpire());
								if(logger.isDebugEnabled())logger.debug(">>auto_cache_process method[{}] update cacheKey:{}",mt.getId(),idCacheKey);
								//插入其他唯一字段引用
								if(insertAction)cacheUniqueSelectRef(args[1], mt, idCacheKey);
								//
								addCurrentThreadCacheKey(idCacheKey);
							}
						}
					}				
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
		Collection<QueryMethodCache> mcs = queryCacheMethods.get(mt.getId().substring(0, mt.getId().lastIndexOf(DOT))).values();
		outter:for (QueryMethodCache methodCache : mcs) {
			if(methodCache.isPk)continue;
			try {	
				Object[] cacheFieldValues = new Object[methodCache.fieldNames.length];
				for (int i = 0; i < cacheFieldValues.length; i++) {
					if(methodCache.fieldNames[i] == null)break outter;
					cacheFieldValues[i] = ReflectUtils.getObjectValue(object, methodCache.fieldNames[i]);
					if(cacheFieldValues[i] == null)continue outter;
				}
				String fieldCacheKey = genarateQueryCacheKey(methodCache.keyPattern , cacheFieldValues);
				
				cacheFieldRefKey(fieldCacheKey,cacheKey, methodCache.getExpire());
				if(logger.isDebugEnabled())logger.debug(">>auto_cache_process method[{}] add ref cacheKey:{}",mt.getId(),fieldCacheKey);
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
	 * 生成查询缓存key
	 * @param cacheInfo
	 * @param param
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private String genarateQueryCacheKey(String keyPattern,Object param){
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
	

	
	private QueryMethodCache getQueryMethodCache(String mtId){
		String key1 = mtId.substring(0, mtId.lastIndexOf(DOT));
		if(queryCacheMethods.containsKey(key1)){
			return queryCacheMethods.get(key1).get(mtId);
		}
		return null;
	}
	
	private QueryMethodCache getQueryByPkMethodCache(String mtId){
		mtId = mtId.substring(0, mtId.lastIndexOf(DOT));
		if(queryCacheMethods.containsKey(mtId)){
			return queryCacheMethods.get(mtId).get(mtId + "." + methodDefine.selectName());
		}
		return null;
	}
	
	
	private boolean skipCache(QueryMethodCache metadata){
		if(!metadata.checkExpired)return false;
		String mapperId = metadata.methodName;
		String contextParamVal;
		if(metadata.contextParam != null){
			contextParamVal = MybatisRuntimeContext.getContextParam(metadata.contextParam);
			if(contextParamVal == null){
				logger.warn(">>auto_cache_process checkSkipCache contextParamValIsNull ->method:{},contextParam:{}",mapperId,metadata.contextParam);
				return true;
			}
		}else{
			contextParamVal = StringUtils.EMPTY;
		}
		String value = String.format(VALUE_TEMPLATE, contextParamVal,mapperId);
		//
		boolean result;
		//可能标记了多个查询方法，所以这里不能remove
		if(StringUtils.isBlank(contextParamVal)){
			result = getCacheProvider().existZsetValue(KEY_NAME, value);
			if(result){				
				result = getCacheProvider().setnx(value, DOT, metadata.expire);
				if(logger.isDebugEnabled()){
					logger.debug(">>auto_cache_process NoContextParam ->checkKey:{},skip:{}",value, result);
				}
			}
		}else{
			result = getCacheProvider().removeZsetValue(KEY_NAME, value);
		}
		if(logger.isDebugEnabled()){
			logger.debug(">>auto_cache_process checkSkipCache ->method:{},checkKey:{},skip:{}",mapperId,value, result);
		}
		return result;
	}

	private void tryRemarkCleanRalationCache(MappedStatement mt){
		String mapperId = mt.getId();
		if(!requiredCheckCacheMethodMapppings.containsKey(mapperId))return;
		List<String> list = requiredCheckCacheMethodMapppings.get(mapperId);
		//
		long score = System.currentTimeMillis() / 1000 - baseScore;
		String contextParam;
		QueryMethodCache metadata;
		for (String queryMapperId : list) {
			metadata = getQueryMethodCache(queryMapperId);
			if(!metadata.checkExpired)continue;
			if(metadata.contextParam != null){
				contextParam = MybatisRuntimeContext.getContextParam(metadata.contextParam);
				if(contextParam == null){
					logger.warn(">>auto_cache_process markCacheExpired contextParamValIsNull ->method:{},queryMethod:{},contextParam:{}",mapperId,queryMapperId,metadata.contextParam);
					return;
				}
			}else{
				contextParam = StringUtils.EMPTY;
			}
			String value = String.format(VALUE_TEMPLATE, contextParam,queryMapperId);
			getCacheProvider().addZsetValue(KEY_NAME, value, score);
			if(metadata.contextParam == null){
				getCacheProvider().remove(value);
			}
			if(logger.isDebugEnabled())logger.debug(">>auto_cache_process markCacheExpired ->method:{}, addKey :{},cleanScore:{}",mapperId,value,score);
		}
	}
	


	@Override
	public void start(JeesuiteMybatisInterceptor context) {
		
		nullValueCache = MybatisConfigs.getBoolean(context.getGroupName(), MybatisConfigs.CACHE_NULL_VALUE, false);
		dynamicCacheTime = MybatisConfigs.getBoolean(context.getGroupName(), MybatisConfigs.CACHE_DYNAMIC_EXPIRE, false);
		defaultCacheExpire = Long.parseLong(MybatisConfigs.getProperty(context.getGroupName(), MybatisConfigs.CACHE_EXPIRE_SECONDS, String.valueOf(IN_1HOUR)));
		
		String crudDriver = MybatisConfigs.getCrudDriver(context.getGroupName());
		if("mapper3".equalsIgnoreCase(crudDriver)){
			methodDefine = new Mapper3CrudMethodDefine();
		}else{
			methodDefine = new DefaultCrudMethodDefine();
		}
		
		logger.info("crudDriver use:{},nullValueCache:{},defaultCacheExpireSeconds:{},dynamicCacheTime:{}",crudDriver,nullValueCache,defaultCacheExpire,dynamicCacheTime);

		List<EntityInfo> entityInfos = MybatisMapperParser.getEntityInfos(context.getGroupName());
		
		Class<BaseEntity> baseEntityClass = BaseEntity.class;
		QueryMethodCache methodCache = null;
		for (EntityInfo ei : entityInfos) {
			if(!baseEntityClass.isAssignableFrom(ei.getEntityClass())){
				logger.warn("[{}] not extends from [{}],ignore register auto cache!!!!",ei.getEntityClass().getName(),baseEntityClass.getName());
				continue;
			}
			Class<?> mapperClass = ei.getMapperClass();
			
			//按主键查询方法定义
			QueryMethodCache queryByPKMethod = generateQueryByPKMethod(mapperClass, ei.getEntityClass());
			
			if(queryByPKMethod == null)continue;
			
            boolean entityWithAnnotation = ei.getEntityClass().isAnnotationPresent(Cache.class);
			
			String keyPatternForPK = queryByPKMethod.keyPattern;

			Map<String, QueryMethodCache> tmpMap = new HashMap<>();
		
			//接口定义的自动缓存方法
			for (MapperMethod method : ei.getMapperMethods()) {
				if(method.getMethod().isAnnotationPresent(Cache.class)){
					if(tmpMap.containsKey(method.getFullName()))continue;
					methodCache = generateQueryMethodCacheByMethod(ei, method);
					tmpMap.put(method.getFullName(), methodCache);
					logger.info("解析查询方法{}自动缓存配置 ok,keyPattern:[{}]",methodCache.methodName,methodCache.keyPattern);
				}
			}
			
			//无任何需要自动缓存的方法
			if(entityWithAnnotation == false && tmpMap.isEmpty()){
				continue;
			}
			//
			if(entityWithAnnotation){
				queryByPKMethod.setExpire(ei.getEntityClass().getAnnotation(Cache.class).expire());
			}
			
			//缓存需要自动缓存的mapper
			cacheEnableMappers.add(ei.getMapperClass().getName());
			//主键查询方法
			tmpMap.put(queryByPKMethod.methodName, queryByPKMethod);
			logger.info("解析查询方法{}自动缓存配置 ok,keyPattern:[{}]",queryByPKMethod.methodName,queryByPKMethod.keyPattern);
			
			queryCacheMethods.put(mapperClass.getName(), tmpMap);
			
			//更新缓存方法
			generateUpdateByPkCacheMethod(mapperClass, ei.getEntityClass(), keyPatternForPK);
		}
		//
		if(queryCacheMethods.isEmpty())return;
		//
		registerClearExpiredKeyTask();
	}
	
	private void registerClearExpiredKeyTask(){
		clearExpiredKeysTimer = Executors.newScheduledThreadPool(1);
		clearExpiredKeysTimer.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				long maxScore = System.currentTimeMillis()/1000 - baseScore - defaultCacheExpire - 3600;
				boolean res = cacheProvider.removeZsetValues(KEY_NAME, 0, maxScore);
				logger.info("CacheRefresher clearExpiredMarkKeysTimer runing:cacheName:{} , score range:0~{} ,result:{}",KEY_NAME,maxScore,res);
			}
		}, 5, 2, TimeUnit.MINUTES);
	}
	
	/**
	 * 生成按主键查询缓存定义
	 * @param mapperClass
	 * @param entityClass
	 * @return
	 */
	private QueryMethodCache generateQueryByPKMethod(Class<?> mapperClass,Class<?> entityClass){
		QueryMethodCache methodCache = null;
		Field[] fields = entityClass.getDeclaredFields();
		//主键key前缀
		for (Field field : fields) {
			if(field.isAnnotationPresent(Id.class)){
				methodCache = new QueryMethodCache();
				methodCache.isPk = true;
				methodCache.collectionResult = false;
				methodCache.keyPattern = entityClass.getSimpleName() + ".id:%s";
				methodCache.methodName = mapperClass.getName() + "." + methodDefine.selectName();
			}
		}
		return methodCache;
	}
	
	private void generateUpdateByPkCacheMethod(Class<?> mapperClass,Class<?> entityClass,String keyPatternForPK){
		String methodName = null;
		//按主键插入
		String[] insertNames = methodDefine.insertName().split(",");
		for (String name : insertNames) {				
			methodName = mapperClass.getName() + "." + name;
			updateCacheMethods.put(methodName, new UpdateByPkMethodCache(entityClass,methodName, keyPatternForPK, SqlCommandType.INSERT));
		}
		
		//按主键更新
		String[] updateNames = methodDefine.updateName().split(",");
		for (String name : updateNames) {				
			methodName = mapperClass.getName() + "." + name;
			updateCacheMethods.put(methodName, new UpdateByPkMethodCache(entityClass,methodName, keyPatternForPK, SqlCommandType.UPDATE));
		}
		
		//按主键删除
		methodName = mapperClass.getName() + "." + methodDefine.deleteName();
		updateCacheMethods.put(methodName, new UpdateByPkMethodCache(entityClass,methodName, keyPatternForPK, SqlCommandType.DELETE));

	}
	
	/**
	 * 按查询方法生成缓存key前缀
	 * @param entityClassName
	 * @param method
	 * @return
	 */
	private QueryMethodCache generateQueryMethodCacheByMethod(EntityInfo entityInfo,MapperMethod mapperMethod){

		Method method = mapperMethod.getMethod();
		Cache cacheAnnotation = method.getAnnotation(Cache.class);
		String[] evictOnMethods = cacheAnnotation.evictOnMethods();
		Class<?> mapperClass = entityInfo.getMapperClass();
		Class<?> entityClass = entityInfo.getEntityClass();
		QueryMethodCache methodCache = new QueryMethodCache();
		methodCache.methodName = mapperClass.getName() + DOT + method.getName();
		methodCache.concurrency = cacheAnnotation.concurrency();
		methodCache.uniqueIndex = cacheAnnotation.uniqueIndex();
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
		methodCache.fieldNames = new String[method.getParameterTypes().length];
		StringBuilder sb = new StringBuilder(entityClass.getSimpleName()).append(DOT).append(method.getName());
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
			sb.append(i == 0 ? ":" : "_").append("%s");
		}
		methodCache.keyPattern = sb.toString();
		
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

				for (MapperMethod mm : methodEntityInfo.getMapperMethods()) {
					if(mm.getSqlType() == SqlCommandType.SELECT)continue;
					if(mm.getFullName().contains(methodName.replace("*", ""))){
						addCacheCheckRelations(mm.getFullName(), method.getFullName());
					}
				}
			}
		}
		
	}
	
	private void addCacheCheckRelations(String updateMethodName,String queryMethodName){
		 List<String> list = requiredCheckCacheMethodMapppings.get(updateMethodName);
		 if(list == null){
			 list = new ArrayList<>();
			 requiredCheckCacheMethodMapppings.put(updateMethodName, list);
		 }
		 list.add(queryMethodName);
	}
	
	/**
	 * 查询缓存方法
	 */
	private class QueryMethodCache{
		String methodName;
		String keyPattern;
		long expire = defaultCacheExpire;//过期时间（秒）
		boolean isPk = false;//主键查询
		boolean uniqueIndex = false;
		boolean collectionResult = false;//查询结果是集合
		String[] fieldNames;//作为查询条件的字段名称
		boolean checkExpired = false; //是否需要检查缓存过期时间
		String contextParam;  
		boolean concurrency = true;
		String refKey;
		public QueryMethodCache() {}
		
		public void setExpire(long expire) {
			this.expire = expire > 0 ? expire : defaultCacheExpire;
		}

		public long getExpire() {
			if(!dynamicCacheTime)return expire;
			//缓存时间加上随机，防止造成缓存同时失效雪崩
			long rnd = RandomUtils.nextLong(0, IN_1HOUR);
			return expire + (rnd > expire ? RandomUtils.nextLong(0, expire) : rnd);
		}
		
		/**
		 * 是否需要通过关联主键二次查询
		 * @return
		 */
		public boolean isSecondQueryById(){
			return isPk == false && uniqueIndex;
		}
	}
	
	/**
	 * 按主键更新（add,update,delete）的方法
	 */
	private class UpdateByPkMethodCache{
		public String keyPattern;
		
		public UpdateByPkMethodCache(Class<?> entityClass,String methodName, String keyPattern, SqlCommandType sqlCommandType) {
			this.keyPattern = keyPattern;
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
		try {			
			clearExpiredKeysTimer.shutdown();
		} catch (Exception e) {}
	}

	@Override
	public int interceptorOrder() {
		return 0;
	}
}
