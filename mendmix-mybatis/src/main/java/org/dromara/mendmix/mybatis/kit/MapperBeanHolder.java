package org.dromara.mendmix.mybatis.kit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.dromara.mendmix.mybatis.core.BaseMapper;
import org.dromara.mendmix.spring.InstanceFactory;

/**
 * 
 * <br>
 * @author vakinge(vakinge)
 * @date 2023年7月8日
 */
@SuppressWarnings("rawtypes")
public class MapperBeanHolder {

	private static Map<String, BaseMapper> instanceCache = new ConcurrentHashMap<>();
	
	public static BaseMapper getMappeBean(String mapperClassName) {
		try {
			BaseMapper mapper = instanceCache.get(mapperClassName);
			if(mapper != null)return mapper;
			Class<?> mapperClass = MybatisMapperParser.getMapperMetadata(mapperClassName).getMapperClass();
			mapper = (BaseMapper) InstanceFactory.getInstance(mapperClass);
			instanceCache.put(mapperClassName, mapper);
			return mapper;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
