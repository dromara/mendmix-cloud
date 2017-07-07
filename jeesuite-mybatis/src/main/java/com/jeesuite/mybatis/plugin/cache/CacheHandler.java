package com.jeesuite.mybatis.plugin.cache;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.mybatis.core.BaseEntity;
import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.crud.builder.SqlTemplate;
import com.jeesuite.mybatis.exception.MybatisHanlerInitException;
import com.jeesuite.mybatis.kit.CacheKeyUtils;
import com.jeesuite.mybatis.kit.ReflectUtils;
import com.jeesuite.mybatis.parser.EntityInfo;
import com.jeesuite.mybatis.parser.MybatisMapperParser;
import com.jeesuite.mybatis.plugin.JeesuiteMybatisInterceptor;
import com.jeesuite.mybatis.plugin.PluginConfig;
import com.jeesuite.mybatis.plugin.cache.annotation.Cache;
import com.jeesuite.mybatis.plugin.cache.annotation.CacheEvictCascade;
import com.jeesuite.mybatis.plugin.cache.name.DefaultCacheMethodDefine;
import com.jeesuite.mybatis.plugin.cache.name.Mapper3CacheMethodDefine;
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

	public final static long IN_5MINS = 60 * 5;
    public final static long IN_1HOUR = 60 * 60;
	public final static long DEFAULT_CACHER_SECONDS = IN_1HOUR * 24;
	
	private static final String STR_PARAM = "param";
	private static final String STR_LIST = "list";
	private static final String STR_COLLECTION = "collection";

	protected static final Logger logger = LoggerFactory.getLogger(CacheHandler.class);

	private static final String ID_CACHEKEY_JOIN = ".id:";
	private static final String WHERE_REGEX = "(w|W)(here|HERE)";
	private static final String QUERY_IDS_SUFFIX = "_ralateIds";
	protected static final String SPLIT_PONIT = ".";
	public static final String GROUPKEY_SUFFIX = "~keys";
	//null缓存占位符（避免频繁查询不存在对象造成缓存穿透导致频繁查询db）
	public static final String NULL_PLACEHOLDER = "~null";
	
	//需要缓存的所有mapper
	private static List<String> cacheEnableMappers = new ArrayList<>();
	
	private static Map<String, String> mapperNameRalateEntityNames = new HashMap<>();
	
	/**
	 * 更新方法关联的缓存组
	 */
	private static Map<String, List<String>> cacheEvictCascades = new HashMap<>();
	
	private static Map<String, Map<String, QueryMethodCache>> queryCacheMethods = new HashMap<>();
	
	private static Map<String, UpdateByPkMethodCache> updateCacheMethods = new HashMap<>();
	
	private static List<String> groupKeys = new ArrayList<>();
	
	//记录当前线程写入的所有缓存key
	private static ThreadLocal<List<String>>  TransactionWriteCacheKeys = new ThreadLocal<>();
	
	protected static CacheProvider cacheProvider;
	
	private CacheMethodDefine methodDefine;
	
	private ScheduledExecutorService clearExpiredGroupKeysTimer;
	
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

		if(mt.getSqlCommandType().equals(SqlCommandType.SELECT)){	
			//按主键查询
			QueryMethodCache cacheInfo = getQueryMethodCache(mt.getId());
			if(cacheInfo == null)return null;
			final String cacheKey = genarateQueryCacheKey(cacheInfo.keyPattern, args[1]);
			
			Object cacheObject = null;
			//按主键查询以及标记非引用关系的缓存直接读取缓存
			if(cacheInfo.isSecondQueryById() == false){
				//从缓存读取
				cacheObject = getCacheProvider().get(cacheKey);
				if(cacheObject != null && logger.isDebugEnabled())logger.debug("_autocache_ method[{}] find result from cacheKey:{}",mt.getId(),cacheKey);
			}else{
				//新根据缓存KEY找到与按ID缓存的KEY
				String cacheKeyById = getCacheProvider().get(cacheKey);
				if(cacheKeyById != null){
					if(NULL_PLACEHOLDER.equals(cacheKeyById)){
						cacheObject = NULL_PLACEHOLDER;
					}else{						
						cacheObject = getCacheProvider().get(cacheKeyById);
						if(cacheObject != null && logger.isDebugEnabled())logger.debug("_autocache_ method[{}] find result from cacheKey:{} ,ref by:{}",mt.getId(),cacheKeyById,cacheKey);
					}
				}
			}
			
			if(cacheObject != null){
				if(NULL_PLACEHOLDER.equals(cacheObject)){
					cacheObject = new ArrayList<>();
				}else if(!(cacheObject instanceof Collection)){						
					cacheObject = new ArrayList<>(Arrays.asList(cacheObject));
				}
			}
			return cacheObject;
		}
		
		return null;
	
	}
	

	@SuppressWarnings("rawtypes")
	@Override
	public void onFinished(Invocation invocation,Object result) {
		Object[] args = invocation.getArgs();
		MappedStatement mt = (MappedStatement)args[0]; 
		
		String mapperNameSpace = mt.getId().substring(0, mt.getId().lastIndexOf(SPLIT_PONIT));
		
		QueryMethodCache cacheInfo = null;
		if(mt.getSqlCommandType().equals(SqlCommandType.SELECT)){	
			if(result == null)return; 
			if((cacheInfo = getQueryMethodCache(mt.getId())) == null)return;
			
			final String cacheKey = genarateQueryCacheKey(cacheInfo.keyPattern, args[1]);
			if(result instanceof List){
				List list = (List)result;
				if(list.isEmpty()){
					getCacheProvider().set(cacheKey,NULL_PLACEHOLDER, IN_5MINS,true);
					return;
				}
				result = cacheInfo.collectionResult ? result : list.get(0);
			}
			//按主键查询以及标记非引用关系的缓存直接读取缓存
			if(cacheInfo.isSecondQueryById() == false){
				if(getCacheProvider().set(cacheKey,result, cacheInfo.getExpire(),false)){
					if(logger.isDebugEnabled())logger.debug("_autocache_ method[{}] put result to cache，cacheKey:{}",mt.getId(),cacheKey);
				}
				//结果为集合的情况，增加key到cacheGroup
				if(cacheInfo.groupRalated){
					getCacheProvider().putGroup(cacheInfo.cacheGroupKey, cacheKey,cacheInfo.getExpire());
					logger.debug("_autocache_ method[{}] add key:[{}] to group key:[{}]",mt.getId(),cacheInfo.cacheGroupKey, cacheKey);
				}else{
					//
					cacheUniqueSelectRef(result, mt, cacheKey);
				}
			}else{
				//之前没有按主键的缓存，增加按主键缓存
				String idCacheKey = genarateQueryCacheKey(getQueryByPkMethodCache(mt.getId()).keyPattern,result);
				
				if(idCacheKey != null && cacheKey != null && getCacheProvider().set(idCacheKey,result, cacheInfo.getExpire(),true) 
						&& getCacheProvider().set(cacheKey,idCacheKey, cacheInfo.getExpire(),true)){
					if(logger.isDebugEnabled())logger.debug("_autocache_ method[{}] put result to cache，cacheKey:{},and add ref cacheKey:{}",mt.getId(),idCacheKey,cacheKey);
				}
			}
		}else{
			if(!cacheEnableMappers.contains(mapperNameSpace))return;
			//返回0，未更新成功
			if(result != null && ((int)result) == 0)return;
			if(updateCacheMethods.containsKey(mt.getId())){
				String cacheByPkKey = null;
				UpdateByPkMethodCache updateMethodCache = updateCacheMethods.get(mt.getId());
				if(updateMethodCache.sqlCommandType.equals(SqlCommandType.DELETE)){
					cacheByPkKey = genarateQueryCacheKey(updateMethodCache.keyPattern,args[1]);
					getCacheProvider().remove(cacheByPkKey);
					if(logger.isDebugEnabled())logger.debug("_autocache_ method[{}] remove cacheKey:{} from cache",mt.getId(),cacheByPkKey);
				}else{
					cacheByPkKey = genarateQueryCacheKey(updateMethodCache.keyPattern,args[1]);
					boolean insertCommond = mt.getSqlCommandType().equals(SqlCommandType.INSERT);
					if(insertCommond || mt.getSqlCommandType().equals(SqlCommandType.UPDATE)){
						if(result != null){
							QueryMethodCache queryByPkMethodCache = getQueryByPkMethodCache(mt.getId());
							getCacheProvider().set(cacheByPkKey,args[1], queryByPkMethodCache.getExpire(),true);
							if(logger.isDebugEnabled())logger.debug("_autocache_ method[{}] update cacheKey:{}",mt.getId(),cacheByPkKey);
							//插入其他唯一字段引用
							if(insertCommond)cacheUniqueSelectRef(args[1], mt, cacheByPkKey);
							//
							addCurrentThreadCacheKey(cacheByPkKey);
						}
					}
				}				
			}else{//按条件删除和更新的情况
				Executor executor = (Executor) invocation.getTarget();
				removeCacheByUpdateConditon(executor, mt, mapperNameSpace, args);
                
			}
			//删除同一cachegroup关联缓存
			removeCacheByGroup(mt.getId(), mapperNameSpace);
		}
	}

	/**
	 * 按更新的查询条件更新缓存
	 * @param executor
	 * @param mt
	 * @param mapperNameSpace
	 * @param args
	 */
	private void removeCacheByUpdateConditon(Executor executor, MappedStatement mt, String mapperNameSpace,
			Object[] args) {
		try {					
			Object parameterObject = args[1];
			EntityInfo entityInfo = MybatisMapperParser.getEntityInfoByMapper(mapperNameSpace);
			MappedStatement statement = getQueryIdsMappedStatementForUpdateCache(mt,entityInfo);
			if(statement == null)return ;
			List<?> idsResult = executor.query(statement, parameterObject, RowBounds.DEFAULT, null);
			if(idsResult != null){
				for (Object id : idsResult) {							
					String cacheKey = entityInfo.getEntityClass().getSimpleName() + ID_CACHEKEY_JOIN + id.toString();
					getCacheProvider().remove(cacheKey);
				}
				if(logger.isDebugEnabled())logger.debug("_autocache_ update Method[{}] executed,remove ralate cache {}.id:[{}]",mt.getId(),entityInfo.getEntityClass().getSimpleName(),idsResult);
			}
		} catch (Exception e) {
			logger.error("_autocache_ update Method[{}] remove ralate cache error",e);
		}
	}
	
    private MappedStatement getQueryIdsMappedStatementForUpdateCache(MappedStatement mt,EntityInfo entityInfo) {
    	String msId = mt.getId() + QUERY_IDS_SUFFIX;
    	
    	MappedStatement statement = null;
    	Configuration configuration = mt.getConfiguration();
    	try {
    		statement = configuration.getMappedStatement(msId);
    		if(statement != null)return statement;
		} catch (Exception e) {}
    	
    	synchronized (configuration) {
    		if(configuration.hasStatement(msId))return configuration.getMappedStatement(msId);
    		
			String sql = entityInfo.getMapperSqls().get(mt.getId());
			if(!sql.toLowerCase().contains(entityInfo.getTableName().toLowerCase())){
				return null;
			}
    		sql = "select "+entityInfo.getIdColumn()+" from "+entityInfo.getTableName()+" WHERE " + sql.split(WHERE_REGEX)[1];
    		sql = String.format(SqlTemplate.SCRIPT_TEMAPLATE, sql);
    		SqlSource sqlSource = configuration.getDefaultScriptingLanguageInstance().createSqlSource(configuration, sql, Object.class);
    		
    		MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, msId, sqlSource,SqlCommandType.SELECT);
    		
    		statementBuilder.resource(mt.getResource());
    		statementBuilder.fetchSize(mt.getFetchSize());
    		statementBuilder.statementType(mt.getStatementType());
    		statementBuilder.parameterMap(mt.getParameterMap());
    		statement = statementBuilder.build();
    		
    		 List<ResultMap> resultMaps = new ArrayList<ResultMap>();
    		 
    		 String id = msId + "-Inline";
    		 ResultMap.Builder builder = new ResultMap.Builder(configuration, id, entityInfo.getIdType(), new ArrayList<ResultMapping>(), true);
    	     resultMaps.add(builder.build());
    	     MetaObject metaObject = SystemMetaObject.forObject(statement);
    	     metaObject.setValue("resultMaps", Collections.unmodifiableList(resultMaps));
    		
    		configuration.addMappedStatement(statement);
    		
    		return statement;
		}
    }

	/**
	 * 删除缓存组
	 * @param mt
	 * @param mapperNameSpace
	 */
	private void removeCacheByGroup(String msId, String mapperNameSpace) {
		//删除cachegroup关联缓存
		String entityName = mapperNameRalateEntityNames.get(mapperNameSpace);
		getCacheProvider().clearGroup(entityName,false);
		logger.debug("_autocache_ method[{}] remove cache Group:{}",msId,entityName);
		//关联缓存
		if(cacheEvictCascades.containsKey(msId)){
			String cacheGroup;
			for (String entity : cacheEvictCascades.get(msId)) {
				cacheGroup = entity + GROUPKEY_SUFFIX;
				getCacheProvider().clearExpiredGroupKeys(entity + GROUPKEY_SUFFIX);
				logger.debug("_autocache_ method[{}] remove Cascade cache Group:[{}]",msId,cacheGroup);
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
		Collection<QueryMethodCache> mcs = queryCacheMethods.get(mt.getId().substring(0, mt.getId().lastIndexOf(SPLIT_PONIT))).values();
		outter:for (QueryMethodCache methodCache : mcs) {
			if(methodCache.isPk || methodCache.groupRalated)continue;
			try {	
				Object[] cacheFieldValues = new Object[methodCache.fieldNames.length];
				for (int i = 0; i < cacheFieldValues.length; i++) {
					if(methodCache.fieldNames[i] == null)continue outter;
					cacheFieldValues[i] = ReflectUtils.getObjectValue(object, methodCache.fieldNames[i]);
					if(cacheFieldValues[i] == null)continue outter;
				}
				String fieldCacheKey = genarateQueryCacheKey(methodCache.keyPattern , cacheFieldValues);
				getCacheProvider().set(fieldCacheKey,cacheKey, methodCache.getExpire(),true);
				if(logger.isDebugEnabled())logger.debug("_autocache_ method[{}] add ref cacheKey:{}",mt.getId(),fieldCacheKey);
			} catch (Exception e) {
				logger.warn("cacheUniqueSelectRef:"+cacheKey,e);
			}
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
		if(param instanceof Map){
			Map<String, Object> map = (Map<String, Object>) param;
			Object[] args = new String[map.size()/2];
			if(map.containsKey(STR_COLLECTION) || map.containsKey(STR_LIST)){
				args[0] = CacheKeyUtils.toString(map.containsKey(STR_COLLECTION) ? map.get(STR_COLLECTION) : map.get(STR_LIST));
			}else{				
				for (int i = 0; i < args.length; i++) {
					args[i] = CacheKeyUtils.toString(map.get(STR_PARAM + (i+1)));
				}
			}
			return String.format(keyPattern, args);
		}else if(param instanceof BaseEntity){
			Serializable id = ((BaseEntity)param).getId();
			if(id != null && !"0".equals(id.toString())){				
				return String.format(keyPattern, ((BaseEntity)param).getId());
			}else{
				return String.format(keyPattern, CacheKeyUtils.toString(param));
			}
		}else if(param instanceof Object[]){
			return String.format(keyPattern, (Object[])param);
		}else{
			return param == null ? keyPattern : String.format(keyPattern, CacheKeyUtils.toString(param));
		}
	}
	

	
	private QueryMethodCache getQueryMethodCache(String mtId){
		String key1 = mtId.substring(0, mtId.lastIndexOf(SPLIT_PONIT));
		if(queryCacheMethods.containsKey(key1)){
			return queryCacheMethods.get(key1).get(mtId);
		}
		return null;
	}
	
	private QueryMethodCache getQueryByPkMethodCache(String mtId){
		mtId = mtId.substring(0, mtId.lastIndexOf(SPLIT_PONIT));
		if(queryCacheMethods.containsKey(mtId)){
			return queryCacheMethods.get(mtId).get(mtId + "." + methodDefine.selectName());
		}
		return null;
	}


	@Override
	public void start(JeesuiteMybatisInterceptor context) {
		
		String crudDriver = context.getProperty(PluginConfig.CRUD_DRIVER,"default");
		if("mapper3".equalsIgnoreCase(crudDriver)){
			methodDefine = new Mapper3CacheMethodDefine();
		}else{
			methodDefine = new DefaultCacheMethodDefine();
		}
		
		logger.info("crudDriver use:{}",crudDriver);

		List<EntityInfo> entityInfos = MybatisMapperParser.getEntityInfos();
		
		QueryMethodCache methodCache = null;
		for (EntityInfo ei : entityInfos) {
			Class<?> mapperClass = ei.getMapperClass();
			
			//按主键查询方法定义
			QueryMethodCache queryByPKMethod = generateQueryByPKMethod(mapperClass, ei.getEntityClass());
			
			if(queryByPKMethod == null)continue;
			
            boolean entityWithAnnotation = ei.getEntityClass().isAnnotationPresent(Cache.class);
			
			Cache annotationCache = null;
			
			String keyPatternForPK = queryByPKMethod.keyPattern;

			Map<String, QueryMethodCache> tmpMap = new HashMap<>();
		
			//接口定义的自动缓存方法
			Method[] methods = mapperClass.getDeclaredMethods();
			for (Method method : methods) {
				String fullMethodName = mapperClass.getName() + SPLIT_PONIT + method.getName();
				if(method.isAnnotationPresent(Cache.class)){
					annotationCache = method.getAnnotation(Cache.class);
					if(tmpMap.containsKey(fullMethodName))continue;
					methodCache = generateQueryMethodCacheByMethod(ei, method);
					methodCache.expire = annotationCache.expire();
					tmpMap.put(fullMethodName, methodCache);
					logger.info("解析查询方法{}自动缓存配置 ok,keyPattern:[{}]",methodCache.methodName,methodCache.keyPattern);
				}else if(method.isAnnotationPresent(CacheEvictCascade.class)){
					CacheEvictCascade cascade = method.getAnnotation(CacheEvictCascade.class);
					if(cascade.cascadeEntities().length > 0){
						List<String> entityNames = new ArrayList<>();
						for (Class<?> clazz : cascade.cascadeEntities()) {
							entityNames.add(clazz.getSimpleName());
						}
						cacheEvictCascades.put(fullMethodName, entityNames);
						logger.info("解析查询方法{}自动关联更新缓存配置 ok,cascadeEntities:[{}]",fullMethodName,entityNames);
					}
				}
			}
			
			//无任何需要自动缓存的方法
			if(entityWithAnnotation == false && tmpMap.isEmpty()){
				continue;
			}
			
			//selectAll
			QueryMethodCache selectAllMethod = generateSelectAllMethod(mapperClass, ei.getEntityClass());
			tmpMap.put(selectAllMethod.methodName, selectAllMethod);
			//
			if(entityWithAnnotation){
				queryByPKMethod.expire = ei.getEntityClass().getAnnotation(Cache.class).expire();
				selectAllMethod.expire = ei.getEntityClass().getAnnotation(Cache.class).expire();
			}
			
			//缓存需要自动缓存的mapper
			cacheEnableMappers.add(ei.getMapperClass().getName());
			//
			mapperNameRalateEntityNames.put(ei.getMapperClass().getName(), ei.getEntityClass().getSimpleName());
			//主键查询方法
			tmpMap.put(queryByPKMethod.methodName, queryByPKMethod);
			logger.info("解析查询方法{}自动缓存配置 ok,keyPattern:[{}]",queryByPKMethod.methodName,queryByPKMethod.keyPattern);
			
			queryCacheMethods.put(mapperClass.getName(), tmpMap);
			
			//更新缓存方法
			generateUpdateByPkCacheMethod(mapperClass, ei.getEntityClass(), keyPatternForPK);
			
			//解析方法标注的sql
			String sql = null;
            for (Method method : methods) {
            	sql = null;
				if(method.isAnnotationPresent(Select.class)){
					sql = method.getAnnotation(Select.class).value()[0];
				}else if(method.isAnnotationPresent(Update.class)){
					sql = method.getAnnotation(Update.class).value()[0];
				}else if(method.isAnnotationPresent(Delete.class)){
					sql = method.getAnnotation(Delete.class).value()[0];
				}
				if(sql != null){	
					String key = ei.getMapperClass().getName() + "." + method.getName();
					ei.getMapperSqls().put(key, sql);
				}
			}
		}
		
		//
		registerClearExpiredGroupKeyTask();
	}
	
	private void registerClearExpiredGroupKeyTask(){
		clearExpiredGroupKeysTimer = Executors.newScheduledThreadPool(1);
		clearExpiredGroupKeysTimer.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				for (String key : groupKeys) {
					try {						
						getCacheProvider().clearExpiredGroupKeys(key);
					} catch (Exception e) {
						logger.warn("_autocache_ clearExpiredGroupKeys for {} error!!",key);
					}
				}
			}
		}, 5, 60, TimeUnit.MINUTES);
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
				methodCache.cacheGroupKey = entityClass.getSimpleName() + GROUPKEY_SUFFIX;
				//
				groupKeys.add(methodCache.cacheGroupKey);
			}
		}
		return methodCache;
	}
	
	private QueryMethodCache generateSelectAllMethod(Class<?> mapperClass,Class<?> entityClass){
		QueryMethodCache methodCache = new QueryMethodCache();
		methodCache.cacheGroupKey = entityClass.getSimpleName() + GROUPKEY_SUFFIX;
		methodCache.methodName = mapperClass.getName() + "." + methodDefine.selectAllName();
		methodCache.keyPattern = entityClass.getSimpleName() + ".all";
		methodCache.isPk = false;
		methodCache.collectionResult = true;
		methodCache.groupRalated = true;
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
	private QueryMethodCache generateQueryMethodCacheByMethod(EntityInfo entityInfo,Method method){

		Class<?> mapperClass = entityInfo.getMapperClass();
		Class<?> entityClass = entityInfo.getEntityClass();
		QueryMethodCache methodCache = new QueryMethodCache();
		String methodName = mapperClass.getName() + SPLIT_PONIT + method.getName();
		methodCache.methodName = methodName;
		methodCache.fieldNames = new String[method.getParameterTypes().length];
		methodCache.cacheGroupKey = entityClass.getSimpleName() + GROUPKEY_SUFFIX;
		//
		methodCache.collectionResult = method.getReturnType() == List.class || method.getReturnType() == Set.class;
		if(methodCache.collectionResult){			
			methodCache.groupRalated = true;
		}else{
			// count等统计查询
			methodCache.groupRalated = method.getReturnType().isAnnotationPresent(Table.class) == false;
		}
		
		StringBuilder sb = new StringBuilder(entityClass.getSimpleName()).append(SPLIT_PONIT).append(method.getName());
		
		Annotation[][] annotations = method.getParameterAnnotations();
		
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
				if(!methodCache.groupRalated && !MybatisMapperParser.entityHasProperty(entityClass, fieldName)){
					throw new MybatisHanlerInitException(String.format("@Cache 查询方法[%s] @Param 绑定实体字段[%s]在实体[%s]不存在", methodName,fieldName,entityClass.getName()));
				}
				methodCache.fieldNames[i] = fieldName;
				
			}else{
				if(!methodCache.groupRalated){
					throw new MybatisHanlerInitException(String.format("unique查询方法[%s] 使用了自动缓存Annotation @Cache,参数必须使用 @Param 绑定实体字段", methodName));
				}
			}
			//
			sb.append(i == 0 ? ":" : "_").append("%s");
		}
		
		if(!methodCache.groupRalated && methodCache.fieldNames.length == 1 && entityInfo.getIdProperty().equals(methodCache.fieldNames[0])){
			throw new MybatisHanlerInitException(String.format("按主键查询方法[%s] 使用了自动缓存Annotation @Cache,请使用默认方法[%s]代替", methodName,methodDefine.selectName()));
			}
		methodCache.keyPattern = sb.toString();
		
		return methodCache;
	}
	
	/**
	 * 查询缓存方法
	 */
	private class QueryMethodCache{
		public String cacheGroupKey;//缓存组key
		public String methodName;
		public String keyPattern;
		public long expire = DEFAULT_CACHER_SECONDS;//过期时间（秒）
		public boolean isPk = false;//主键查询
		public boolean collectionResult = false;//查询结果是集合
		public boolean groupRalated = false; //是否需要关联group
		public String[] fieldNames;//作为查询条件的字段名称
		public QueryMethodCache() {}
		//缓存时间加上随机，防止造成缓存同时失效雪崩
		public long getExpire() {
			long rnd = RandomUtils.nextLong(0, expire/3);
			return expire + (rnd > IN_1HOUR ? IN_1HOUR : rnd);
		}



		/**
		 * 是否需要通过关联主键二次查询
		 * @return
		 */
		public boolean isSecondQueryById(){
			return isPk == false && groupRalated == false;
		}
	}
	
	/**
	 * 按主键更新（add,update,delete）的方法
	 */
	private class UpdateByPkMethodCache{
		public String keyPattern;
		public SqlCommandType sqlCommandType;
		
		public UpdateByPkMethodCache(Class<?> entityClass,String methodName, String keyPattern, SqlCommandType sqlCommandType) {
			this.keyPattern = keyPattern;
			this.sqlCommandType = sqlCommandType;
		}
	}
	
	private void addCurrentThreadCacheKey(String key){
		List<String> keys =  TransactionWriteCacheKeys.get();
		if(keys == null){
			keys = new ArrayList<>();
			 TransactionWriteCacheKeys.set(keys);
		}
		keys.add(key);
	}
	
	/**
	 * 回滚当前事务写入的缓存
	 */
	public static void rollbackCache(){
		List<String> keys =  TransactionWriteCacheKeys.get();
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
			clearExpiredGroupKeysTimer.shutdown();
		} catch (Exception e) {}
	}

	@Override
	public int interceptorOrder() {
		return 0;
	}
}
