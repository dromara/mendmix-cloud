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
import com.jeesuite.mybatis.plugin.cache.annotation.CacheKey;


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
	
	private CacheProvider cacheProvider;
	
	public void setCacheProvider(CacheProvider cacheProvider) {
		this.cacheProvider = cacheProvider;
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
			if(result instanceof List){
				List list = (List)result;
				if(list.isEmpty() || list.size() > 1)return;
				result = list.get(0);
			}
	
			cacheInfo = getQueryMethodCache(mt.getId());
			if(cacheInfo == null)return;
			
			final String cacheKey = genarateQueryCacheKey(cacheInfo.keyPattern, args[1]);
			//按主键查询以及标记非引用关系的缓存直接读取缓存
			if(cacheInfo.isPk || !cacheInfo.uniqueResult){
				if(cacheProvider.set(cacheKey,result, cacheInfo.expire)){
					if(logger.isDebugEnabled())logger.debug("method[{}] put result to cache，cacheKey:{}",mt.getId(),cacheKey);
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
				
			}else{
				//TODO 非按主键的
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
			return String.format(keyPattern, param.toString());
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
	
	private static String selectByPrimaryKey = ".selectByPrimaryKey";
	private QueryMethodCache getQueryByPkMethodCache(String mtId){
		mtId = mtId.substring(0, mtId.lastIndexOf(SPLIT_PONIT));
		if(queryCacheMethods.containsKey(mtId)){
			return queryCacheMethods.get(mtId).get(mtId + selectByPrimaryKey);
		}
		return null;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
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
			//实体有缓存标注
			if(entityWithAnnotation){
				annotationCache = ei.getEntityClass().getAnnotation(Cache.class);
				//根据实体field查询方法，主键及带CacheKey的字段
				Field[] fields = ei.getEntityClass().getDeclaredFields();
				//主键key前缀
				for (Field field : fields) {
					methodCache = null;
					if(field.isAnnotationPresent(CacheKey.class)){
						methodCache = new QueryMethodCache();
						methodCache.keyPattern = ei.getEntityClass().getSimpleName() + SPLIT_PONIT + field.getName() + REGEX_PLACEHOLDER;
						methodCache.uniqueResult = true;
						methodCache.methodName = mapperClass.getName() + ".findBy" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
						methodCache.fieldNames = new String[]{field.getName()};
						methodCache.expire = annotationCache.expire();
						tmpMap.put(methodCache.methodName, methodCache);
						logger.info("解析查询方法{}自动缓存配置 ok,keyPattern:[{}]",methodCache.methodName,methodCache.keyPattern);
					}
				}
			}
		
			
			//接口定义的自动缓存方法
			Method[] methods = mapperClass.getDeclaredMethods();
			for (Method method : methods) {
				if(method.isAnnotationPresent(Cache.class)){
					annotationCache = annotationCache != null ? annotationCache : method.getAnnotation(Cache.class);
					String fullMethodName = mapperClass.getName() + SPLIT_PONIT + method.getName();
					if(tmpMap.containsKey(fullMethodName))continue;
					methodCache = generateQueryMethodCacheByMethod(mapperClass, ei.getEntityClass(), method);
					methodCache.expire = annotationCache.expire();
					tmpMap.put(fullMethodName, methodCache);
					logger.info("解析查询方法{}自动缓存配置 ok,keyPattern:[{}]",methodCache.methodName,methodCache.keyPattern);
				}
			}
			
			//无任何需要自动缓存的方法
			if(entityWithAnnotation == false && tmpMap.isEmpty()){
				continue;
			}
			
			//缓存需要自动缓存的实体类名
			cacheEntityClassNames.add(ei.getEntityClass().getSimpleName());
			//主键查询方法
			tmpMap.put(queryByPKMethod.methodName, queryByPKMethod);
			logger.info("解析查询方法{}自动缓存配置 ok,keyPattern:[{}]",queryByPKMethod.methodName,queryByPKMethod.keyPattern);
			
			queryCacheMethods.put(mapperClass.getName(), tmpMap);
			//按主键插入
			String methodName = mapperClass.getName() + ".insert";
			updateCacheMethods.put(methodName, new UpdateByPkMethodCache(methodName, keyPatternForPK, SqlCommandType.INSERT));
			methodName = mapperClass.getName() + ".insertSelective";
			updateCacheMethods.put(methodName, new UpdateByPkMethodCache(methodName, keyPatternForPK, SqlCommandType.INSERT));
			
			//按主键更新
			methodName = mapperClass.getName() + ".updateByPrimaryKey";
			updateCacheMethods.put(methodName, new UpdateByPkMethodCache(methodName, keyPatternForPK, SqlCommandType.UPDATE));
			methodName = mapperClass.getName() + ".updateByPrimaryKeySelective";
			updateCacheMethods.put(methodName, new UpdateByPkMethodCache(methodName, keyPatternForPK, SqlCommandType.UPDATE));
			
			//按主键删除
			methodName = mapperClass.getName() + ".deleteByPrimaryKey";
			updateCacheMethods.put(methodName, new UpdateByPkMethodCache(methodName, keyPatternForPK, SqlCommandType.DELETE));

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
				methodCache.methodName = mapperClass.getName() + ".selectByPrimaryKey";
			}
		}
		
		return methodCache;
	}
	
	/**
	 * 按查询方法生成缓存key前缀
	 * @param entityClassName
	 * @param method
	 * @return
	 */
	private QueryMethodCache generateQueryMethodCacheByMethod(Class<?> mapperClass,Class<?> entityClass,Method method){

		QueryMethodCache methodCache = new QueryMethodCache();
		methodCache.uniqueResult = true;
		String methodName = mapperClass.getName() + SPLIT_PONIT + method.getName();
		methodCache.methodName = methodName;
		methodCache.fieldNames = new String[method.getParameterCount()];
		
		StringBuilder sb = new StringBuilder(entityClass.getSimpleName());
		
		Annotation[][] annotations = method.getParameterAnnotations();
		
		if(annotations != null){
			for (int i = 0; i < annotations.length; i++) {
				Annotation[] aa = annotations[i];
				if(aa.length > 0){
					String fieldName = null;
					for (Annotation annotation : aa) {
						if(annotation.toString().contains(CacheKey.class.getName())){
							fieldName = ((CacheKey)annotation).value();
						}else if(annotation.toString().contains(Param.class.getName())){
							if(fieldName == null)fieldName = ((Param)annotation).value();
						}
					}
					if(fieldName == null)new RuntimeException(String.format("无cacheField 定义 at %s.%s", entityClass.getName(),fieldName,mapperClass.getName(),method.getName()));
					if(!MybatisMapperParser.entityHasProperty(entityClass, fieldName)){
						throw new RuntimeException(String.format("%s无%s属性 at %s.%s", entityClass.getName(),fieldName,mapperClass.getName(),method.getName()));
					}
					sb.append(SPLIT_PONIT).append(fieldName).append(REGEX_PLACEHOLDER);
					methodCache.fieldNames[i] = fieldName;
				}else{
					throw new RuntimeException(String.format("接口%s.%s参数缺失CacheKey标注", mapperClass.getName(),method.getName()));
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
		public String methodName;
		public String keyPattern;
		public long expire;//过期时间（秒）
		public boolean isPk = false;//主键查询
		public boolean uniqueResult = false;//查询结果是否唯一记录
		public String[] fieldNames;//作为查询条件的字段名称
	}
	
	/**
	 * 按主键更新（add,update,delete）的方法
	 */
	private class UpdateByPkMethodCache{
		public String keyPattern;
		public SqlCommandType sqlCommandType;
		
		public UpdateByPkMethodCache(String methodName, String keyPattern, SqlCommandType sqlCommandType) {
			super();
			this.keyPattern = keyPattern;
			this.sqlCommandType = sqlCommandType;
		}
		
		
	}
}
