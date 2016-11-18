package com.jeesuite.mybatis.plugin.cache;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Id;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.jeesuite.mybatis.core.BaseEntity;
import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.core.InterceptorType;
import com.jeesuite.mybatis.kit.ReflectUtils;
import com.jeesuite.mybatis.parser.EntityInfo;
import com.jeesuite.mybatis.parser.MybatisMapperParser;
import com.jeesuite.mybatis.plugin.cache.annotation.Cache;
import com.jeesuite.mybatis.plugin.cache.name.DefaultCacheMethodDefine;
import com.jeesuite.mybatis.plugin.cache.name.Mapper3CacheMethodDefine;
import com.jeesuite.mybatis.plugin.cache.name.MybatisPlusCacheMethodDefine;
import com.jeesuite.mybatis.plugin.cache.provider.DefaultCacheProvider;


/**
 * 自动缓存拦截处理
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年12月7日
 * @Copyright (c) 2015, jwww
 */
public class CacheHandler implements InterceptorHandler,InitializingBean {

	/**
	 * 
	 */
	private static final String SPLIT_PONIT = ".";
	private static final String REGEX_PLACEHOLDER = ":%s";
	private static final String REGEX_POINT = "\\.";

	protected static final Logger logger = LoggerFactory.getLogger(CacheHandler.class);
	
	private static List<String> cacheEntityClassNames = new ArrayList<>();
	
	private static Map<String, Map<String, QueryMethodCache>> queryCacheMethods = new HashMap<>();
	
	private static Map<String, UpdateByPkMethodCache> updateCacheMethods = new HashMap<>();
	
	//记录当前线程写入的所有缓存key
	private static ThreadLocal<List<String>> currentThreadAutoCacheKeys = new ThreadLocal<>();
	
	private CacheProvider cacheProvider;
	
	private CacheMethodDefine methodDefine;
	
	//CRUD框架驱动 default，mapper3，mybatis-plus
	private String crudDriver = "mapper3";
	
	public void setCacheProvider(CacheProvider cacheProvider) {
		this.cacheProvider = cacheProvider;
	}

	public void setCrudDriver(String crudDriver) {
		this.crudDriver = crudDriver;
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
			//按主键查询以及标记非引用关系的缓存直接读取缓存
			if(cacheInfo.isPk || !cacheInfo.uniqueResult){
				//从缓存读取
				Object object = cacheProvider.get(cacheKey);
				if(object != null){
					object = new ArrayList<>(Arrays.asList(object));
					if(logger.isDebugEnabled())logger.debug("method[{}] find result from cacheKey:{}",mt.getId(),cacheKey);
				}
				return object;
			}else{
				//新根据缓存KEY找到与按ID缓存的KEY
				String cacheKeyById = cacheProvider.get(cacheKey);
				if(cacheKeyById == null)return null;
				Object object = cacheProvider.get(cacheKeyById);
				if(object != null){
					object = new ArrayList<>(Arrays.asList(object));
					if(logger.isDebugEnabled())logger.debug("method[{}] find result from cacheKey:{} ,ref by:{}",mt.getId(),cacheKeyById,cacheKey);
				}
				
				return object;
			}
		}
		
		return null;
	
	}
	

	@SuppressWarnings("rawtypes")
	@Override
	public void onFinished(Invocation invocation,Object result) {
		Object[] args = invocation.getArgs();
		MappedStatement mt = (MappedStatement)args[0]; 
		
		QueryMethodCache cacheInfo = null;
		if(mt.getSqlCommandType().equals(SqlCommandType.SELECT)){	
			if(result == null)return;  
			if((cacheInfo = getQueryMethodCache(mt.getId())) == null)return;
			
			if(result instanceof List){
				List list = (List)result;
				if(list.isEmpty())return;
				result = cacheInfo.uniqueResult ? list.get(0) : result;
			}
			final String cacheKey = genarateQueryCacheKey(cacheInfo.keyPattern, args[1]);
			//按主键查询以及标记非引用关系的缓存直接读取缓存
			if(cacheInfo.isPk || !cacheInfo.uniqueResult){
				if(cacheProvider.set(cacheKey,result, cacheInfo.expire)){
					if(logger.isDebugEnabled())logger.debug("method[{}] put result to cache，cacheKey:{}",mt.getId(),cacheKey);
				}
				//结果为集合的情况，增加key到cacheGroup
				if(!cacheInfo.uniqueResult){
					cacheProvider.putGroupKeys(cacheInfo.cacheGroupKey, cacheKey,cacheInfo.expire);
				}
			}else{
				//之前没有按主键的缓存，增加按主键缓存
				String idCacheKey = genarateQueryCacheKey(getQueryByPkMethodCache(mt.getId()).keyPattern,result);
				
				if(idCacheKey != null && cacheKey != null && cacheProvider.set(idCacheKey,result, cacheInfo.expire) 
						&& cacheProvider.set(cacheKey,idCacheKey, cacheInfo.expire)){
					if(logger.isDebugEnabled())logger.debug("method[{}] put result to cache，cacheKey:{},and add ref cacheKey:{}",mt.getId(),idCacheKey,cacheKey);
				}
			}
		}else{
			if(updateCacheMethods.containsKey(mt.getId())){
				String cacheByPkKey = null;
				UpdateByPkMethodCache updateMethodCache = updateCacheMethods.get(mt.getId());
				if(updateMethodCache.sqlCommandType.equals(SqlCommandType.DELETE)){
					cacheByPkKey = genarateQueryCacheKey(updateMethodCache.keyPattern,args[1]);
					cacheProvider.remove(cacheByPkKey);
					if(logger.isDebugEnabled())logger.debug("method[{}] remove cacheKey:{} from cache",mt.getId(),cacheByPkKey);
					//TODO 清除关联缓存
					
				}else{
					cacheByPkKey = genarateQueryCacheKey(updateMethodCache.keyPattern,args[1]);
					boolean insertCommond = mt.getSqlCommandType().equals(SqlCommandType.INSERT);
					if(insertCommond || mt.getSqlCommandType().equals(SqlCommandType.UPDATE)){
						if(result != null){
							QueryMethodCache queryByPkMethodCache = getQueryByPkMethodCache(mt.getId());
							cacheProvider.set(cacheByPkKey,args[1], queryByPkMethodCache.expire);
							if(logger.isDebugEnabled())logger.debug("method[{}] update cacheKey:{}",mt.getId(),cacheByPkKey);
							//插入其他唯一字段引用
							if(insertCommond)cacheUniqueSelectRef(args[1], mt, cacheByPkKey);
						}
					}
				}				
				//TODO 删除同一cachegroup关联缓存
				cacheProvider.clearGroupKeys(updateMethodCache.cacheGroupKey);
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
			if(methodCache.isPk || !methodCache.uniqueResult)continue;
			try {	
				Object[] cacheFieldValues = new Object[methodCache.fieldNames.length];
				for (int i = 0; i < cacheFieldValues.length; i++) {
					cacheFieldValues[i] = ReflectUtils.getObjectValue(object, methodCache.fieldNames[i]);
					if(cacheFieldValues[i] == null)continue outter;
				}
				String fieldCacheKey = genarateQueryCacheKey(methodCache.keyPattern , cacheFieldValues);
				cacheProvider.set(fieldCacheKey,cacheKey, methodCache.expire);
				if(logger.isDebugEnabled())logger.debug("method[{}] add ref cacheKey:{}",mt.getId(),fieldCacheKey);
			} catch (Exception e) {
				e.printStackTrace();
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
			//DeviceEntity.deviceId:%s.userId:%s
			Object[] args = new String[map.size()];
			String[] parts = keyPattern.split(REGEX_POINT);
			for (int i = 1; i < parts.length; i++) {
				args[i-1] = map.get(parts[i].replace(REGEX_PLACEHOLDER, "")).toString();
			}
			return String.format(keyPattern, args);
		}else if(param instanceof BaseEntity){
			return String.format(keyPattern, ((BaseEntity)param).getId());
		}else if(param instanceof Object[]){
			return String.format(keyPattern, (Object[])param);
		}else{
			return param == null ? keyPattern : String.format(keyPattern, param.toString());
		}
	}
	

	@Override
	public InterceptorType getInterceptorType() {
		return InterceptorType.around;
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
	public void afterPropertiesSet() throws Exception {
		if("mapper3".equalsIgnoreCase(crudDriver)){
			methodDefine = new Mapper3CacheMethodDefine();
		}else if("mybatis-plus".equalsIgnoreCase(crudDriver)){
			methodDefine = new MybatisPlusCacheMethodDefine();
		}else{
			methodDefine = new DefaultCacheMethodDefine();
		}
		
		if(cacheProvider == null)cacheProvider = new DefaultCacheProvider();
		
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
				if(method.isAnnotationPresent(Cache.class)){
					annotationCache = method.getAnnotation(Cache.class);
					String fullMethodName = mapperClass.getName() + SPLIT_PONIT + method.getName();
					if(tmpMap.containsKey(fullMethodName))continue;
					methodCache = generateQueryMethodCacheByMethod(mapperClass, ei.getEntityClass(), method);
					methodCache.expire = annotationCache.expire();
					methodCache.uniqueResult = annotationCache.uniqueResult();
					tmpMap.put(fullMethodName, methodCache);
					logger.info("解析查询方法{}自动缓存配置 ok,keyPattern:[{}]",methodCache.methodName,methodCache.keyPattern);
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
			
			//缓存需要自动缓存的实体类名
			cacheEntityClassNames.add(ei.getEntityClass().getSimpleName());
			//主键查询方法
			tmpMap.put(queryByPKMethod.methodName, queryByPKMethod);
			logger.info("解析查询方法{}自动缓存配置 ok,keyPattern:[{}]",queryByPKMethod.methodName,queryByPKMethod.keyPattern);
			
			queryCacheMethods.put(mapperClass.getName(), tmpMap);
			
			//更新缓存方法
			generateUpdateCacheMethod(mapperClass, ei.getEntityClass(), keyPatternForPK);
		}
		
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
				methodCache.uniqueResult = true;
				methodCache.keyPattern = entityClass.getSimpleName() + ".id:%s";
				methodCache.methodName = mapperClass.getName() + "." + methodDefine.selectName();
				methodCache.cacheGroupKey = entityClass.getSimpleName() + "~keys";
			}
		}
		return methodCache;
	}
	
	private QueryMethodCache generateSelectAllMethod(Class<?> mapperClass,Class<?> entityClass){
		QueryMethodCache methodCache = new QueryMethodCache();
		methodCache.cacheGroupKey = entityClass.getSimpleName() + "~keys";
		methodCache.methodName = mapperClass.getName() + "." + methodDefine.selectAllName();
		methodCache.keyPattern = entityClass.getSimpleName() + ".all";
		methodCache.isPk = false;
		methodCache.uniqueResult = false;
		return methodCache;
	}
	
	private void generateUpdateCacheMethod(Class<?> mapperClass,Class<?> entityClass,String keyPatternForPK){
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
	private QueryMethodCache generateQueryMethodCacheByMethod(Class<?> mapperClass,Class<?> entityClass,Method method){

		QueryMethodCache methodCache = new QueryMethodCache();
		String methodName = mapperClass.getName() + SPLIT_PONIT + method.getName();
		methodCache.methodName = methodName;
		methodCache.fieldNames = new String[method.getParameterTypes().length];
		methodCache.cacheGroupKey = entityClass.getSimpleName() + "~keys";
		
		StringBuilder sb = new StringBuilder(entityClass.getSimpleName());
		
		Annotation[][] annotations = method.getParameterAnnotations();
		
		if(annotations != null){
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
					if(fieldName == null)new RuntimeException(String.format("无cacheField 定义 at %s.%s", entityClass.getName(),fieldName,mapperClass.getName(),method.getName()));
					if(!MybatisMapperParser.entityHasProperty(entityClass, fieldName)){
						throw new RuntimeException(String.format("%s无%s属性 at %s.%s", entityClass.getName(),fieldName,mapperClass.getName(),method.getName()));
					}
					sb.append(SPLIT_PONIT).append(fieldName).append(REGEX_PLACEHOLDER);
					methodCache.fieldNames[i] = fieldName;
				}else{
					throw new RuntimeException(String.format("接口%s.%s参数缺失Param标注", mapperClass.getName(),method.getName()));
				}
			}
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
		public long expire = CacheExpires.IN_1WEEK;//过期时间（秒）
		public boolean isPk = false;//主键查询
		public boolean uniqueResult = false;//查询结果是否唯一记录
		public String[] fieldNames;//作为查询条件的字段名称
		public QueryMethodCache() {}
	}
	
	/**
	 * 按主键更新（add,update,delete）的方法
	 */
	private class UpdateByPkMethodCache{
		public String cacheGroupKey;//缓存组key
		public String keyPattern;
		public SqlCommandType sqlCommandType;
		
		public UpdateByPkMethodCache(Class<?> entityClass,String methodName, String keyPattern, SqlCommandType sqlCommandType) {
			this.cacheGroupKey = entityClass.getSimpleName() + "~keys";
			this.keyPattern = keyPattern;
			this.sqlCommandType = sqlCommandType;
		}
	}
	
	public static void rollbackCache(){
		List<String> keys = currentThreadAutoCacheKeys.get();
		if(keys == null)return;
		for (String key : keys) {
			
		}
	}
}
