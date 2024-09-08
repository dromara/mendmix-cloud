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
package org.dromara.mendmix.common.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.dromara.mendmix.common.json.serializer.DateConvertSerializer;

/**
 * Bean复制<br>
 * Copyright (c) 2015, vakinge@gmail.com.
 */
public class BeanUtils {
	
	private static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix");

    private static final String CLASS_PROP_NAME = "class";

	private static Map<String, Map<String, PropertyDescriptor>> cache = new ConcurrentHashMap<>();

    private static Map<String, List<String>> fieldCache = new HashMap<>();
    
    private static List<String> dynaProxyClassKeys = Arrays.asList("JavassistProxyFactory$");
    
    private static List<String> jdkPackagePrefixs = Arrays.asList("java.");
    
    private static boolean ignoreNull = ResourceUtils.getBoolean("mendmix-cloud.beancopy.ignoreNull", true);
    private static boolean deepCopy = ResourceUtils.getBoolean("mendmix-cloud.beancopy.deepCopy", true);
    
    /**
     * 值复制
     *
     * @param src
     * @param dest
     * @param deepCopy 是否深拷贝
     * @param ignoreNull 是否忽略null值
     * @return
     * @throws RuntimeException
     */
    public static <T> T copy(Object src, T dest, boolean deepCopy, boolean ignoreNull) throws RuntimeException {
        if (src == null)
            return null;

        try {
            Class<? extends Object> destClass = dest.getClass();
            Map<String, PropertyDescriptor> srcDescriptors = getCachePropertyDescriptors(src.getClass());
            Map<String, PropertyDescriptor> destDescriptors = getCachePropertyDescriptors(destClass);

            Set<String> keys = destDescriptors.keySet();
            for (String key : keys) {
                PropertyDescriptor srcDescriptor = srcDescriptors.get(key);

                if (srcDescriptor == null)
                    continue;

                PropertyDescriptor destDescriptor = destDescriptors.get(key);

                Object value = srcDescriptor.getReadMethod().invoke(src);

                Class<?> propertyType = destDescriptor.getPropertyType();

                Method writeMethod = destDescriptor.getWriteMethod();
                String name = destDescriptor.getName();
                if (writeMethod == null) {
                    try {
                        writeMethod = destClass.getMethod("set" + name.substring(0, 1).toUpperCase() + name.substring(1), destDescriptor.getPropertyType());
                        destDescriptor.setWriteMethod(writeMethod);
                    } catch (Exception e) {
                    }
                }
                if(writeMethod == null)continue;
                
                if (value != null) {
                	if(srcDescriptor.getPropertyType().isEnum() || isSimpleDataType(srcDescriptor.getPropertyType())){ 
                		//类型不匹配
                		if( propertyType != srcDescriptor.getPropertyType()) {   
                			value = toAdaptTypeValue(srcDescriptor.getPropertyType(), value, propertyType);
                		}
                	}else{
                		//非简单类型 对象递归复制
                		try {
                			if(List.class.isAssignableFrom(propertyType)) {
                    			List<?> srcList = (List<?>)value;
                    			if(srcList != null && !srcList.isEmpty()) {
                    				Class<?> srcGenericClass = srcList.get(0).getClass();
                        			if(!isSimpleDataType(srcGenericClass)) {
                        				Class<?> distGenericType = getGenericType(destClass, name);
                        				if(distGenericType != null && (deepCopy || srcGenericClass != distGenericType)) {
                        					value = copy(srcList, distGenericType);
                        				}
                        			}
                    			}
                    		}else if(deepCopy && !jdkPackagePrefixs.stream().anyMatch(prefix -> propertyType.getName().startsWith(prefix))){
                    			value = copy(value, propertyType);
                    		}
						} catch (Exception e) {
							logger.error("copy object[{}.{}]error:{}", src.getClass().getName(),name,ExceptionFormatUtils.buildExceptionMessages(e, 3));
						}
                	}
                    //
                	writeMethod.invoke(dest, value);
                }else if(!ignoreNull) {
                	try {writeMethod.invoke(dest, value);} catch (Exception e) {}
                }
            }
            return dest;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <T> T copy(Object src, T dest, boolean deepCopy) throws RuntimeException {
    	return copy(src, dest, deepCopy, ignoreNull);
    }
    
    public static <T> T merge(Object src, T dest) throws RuntimeException {
        return copy(src, dest, deepCopy,true);
    }

    public static <T> T copy(Object src, T dest) throws RuntimeException {
        return copy(src, dest, deepCopy,ignoreNull);
    }

    /**
     * 
     * @param srcs
     * @param destClass
     * @param deepCopy  是否深拷贝
     * @return
     */
    public static <T> List<T> copy(List<?> srcs, Class<T> destClass, boolean deepCopy) {
        if (srcs == null)
            return new ArrayList<T>();

        List<T> dests = new ArrayList<T>(srcs.size());
        for (Object src : srcs) {
            dests.add(copy(src, destClass, deepCopy));
        }

        return dests;
    }


    public static <T> List<T> copy(List<?> srcs, Class<T> destClass) {
        return copy(srcs, destClass, deepCopy);
    }

    public static <T> T copy(Object src, Class<T> destClass, boolean deepCopy) throws RuntimeException {
        if (src == null)
            return null;
        try {
            T dest = destClass.newInstance();
            copy(src, dest, deepCopy, ignoreNull);
            return dest;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 深拷贝
     * @param src
     * @param destClass
     * @return
     * @throws RuntimeException
     */
    public static <T> T copy(Object src, Class<T> destClass) throws RuntimeException {
        return copy(src, destClass, deepCopy);
    }

    /**
     * 把对象值为0的包装类型属性转为null
     *
     * @param bean
     * @param excludeFields 排除不处理的字段
     * @throws RuntimeException
     */
    public static void zeroWrapPropertiesToNull(Object bean, String... excludeFields) throws RuntimeException {
        try {
            Map<String, PropertyDescriptor> srcDescriptors = getCachePropertyDescriptors(bean.getClass());
            Set<String> keys = srcDescriptors.keySet();

            List<String> excludeFieldsList = null;
            if (excludeFields != null && excludeFields.length > 0 && StringUtils.isNotBlank(excludeFields[0])) {
                excludeFieldsList = Arrays.asList(excludeFields);
            }

            for (String key : keys) {
                PropertyDescriptor srcDescriptor = srcDescriptors.get(key);
                if (srcDescriptor == null) continue;
                if (excludeFieldsList != null && excludeFieldsList.contains(key)) continue;
                Object value = srcDescriptor.getReadMethod().invoke(bean);

                boolean isWrapType = srcDescriptor.getPropertyType() == Long.class || srcDescriptor.getPropertyType() == Integer.class || srcDescriptor.getPropertyType() == Short.class || srcDescriptor.getPropertyType() == Double.class || srcDescriptor.getPropertyType() == Float.class;
                if (isWrapType && value != null && Integer.parseInt(value.toString()) == 0) {
                    value = null;
                    Method writeMethod = srcDescriptor.getWriteMethod();
                    if (writeMethod != null) writeMethod.invoke(bean, value);
                }

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    

    private static Object toAdaptTypeValue(Class<?> srcPropertyType, Object value, Class<?> distPropertyType) {

    	if (distPropertyType == BigDecimal.class) {
            value = (value == null) ? BigDecimal.ZERO : new BigDecimal(value.toString().trim());
        } else if (distPropertyType == byte.class || distPropertyType == Byte.class) {
            value = (value == null) ? Byte.valueOf("0") : Byte.valueOf(value.toString().trim());
        } else if (distPropertyType == short.class || distPropertyType == Short.class) {
            value = (value == null) ? Short.valueOf("0") : Short.valueOf(value.toString().trim());
        } else if (distPropertyType == int.class || distPropertyType == Integer.class) {
            if (srcPropertyType == boolean.class || srcPropertyType == Boolean.class) {
                value = Boolean.parseBoolean(value.toString().trim()) ? 1 : 0;
            } else {
                value = (value == null) ? Integer.valueOf("0") : Integer.valueOf(value.toString().trim());
            }
        } else if (distPropertyType == double.class || distPropertyType == Double.class) {
            value = (value == null) ? Double.valueOf("0") : Double.valueOf(value.toString().trim());
        } else if (distPropertyType == Date.class) {
        	if(value != null){
        		if(srcPropertyType == String.class){
        			value = DateUtils.parseDate(value.toString().trim());
        		}else if (srcPropertyType == Long.class || srcPropertyType == Integer.class || srcPropertyType == long.class || srcPropertyType == int.class) {
                    Long val = Long.valueOf(value.toString().trim());
                    if (val.longValue() != 0)
                        value = new Date(val);
                    else
                        value = null;
                }
        	}
            
        } else if (distPropertyType == String.class && srcPropertyType != String.class) {
            if (value != null) {
            	if(srcPropertyType == Date.class){
            		value = DateUtils.format((Date)value);
            	}else{            		
            		value = value.toString();
            	}
            }
        } else if (distPropertyType == boolean.class || distPropertyType == Boolean.class) {
        	value = Boolean.parseBoolean(value.toString()) || "1".equals(value.toString());
        } 
    	
        return value;
    }

    public static Object toPrimitiveValue(String value,Class<?> propertyType){
    	Object result = value;
    	if (propertyType == BigDecimal.class) {
    		result = new BigDecimal(value);
        } else if (propertyType == byte.class || propertyType == Byte.class) {
        	result = Byte.valueOf(value);
        } else if (propertyType == short.class || propertyType == Short.class) {
        	result = Short.valueOf(value.toString());
        } else if (propertyType == int.class || propertyType == Integer.class) {
        	result = Integer.parseInt(value);
        } else if (propertyType == double.class || propertyType == Double.class) {
        	result = Double.valueOf(value.toString());
        } else if (propertyType == long.class || propertyType == Long.class) {
        	result = Long.valueOf(value.toString());
        } else if (propertyType == Date.class) {
        	if(value != null){
        		result = DateUtils.parseDate(value);
        	}
        } else if (propertyType == LocalDate.class) {
        	if(value != null){
        		result = LocalDate.parse(value);
        	}
        } else if (propertyType == boolean.class || propertyType == Boolean.class) {
        	result = Boolean.parseBoolean(value) || "1".equals(value);
        } 
    	return result;
    }

    public static <T> T mapToBean(Map<String, Object> map, Class<T> clazz) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        
        String propertyName = null;
        try {	   	
        	T bean = clazz.newInstance();
        	Map<String, PropertyDescriptor> descriptors = getCachePropertyDescriptors(clazz);
        	for (PropertyDescriptor descriptor : descriptors.values()) {
        		propertyName = descriptor.getName();
        		if(map.containsKey(propertyName)){
        			Object object = map.get(propertyName);
					if(object == null)continue;
					if(descriptor.getPropertyType() != object.getClass() 
							&& !descriptor.getPropertyType().isAssignableFrom(object.getClass())){						
						object = toPrimitiveValue(object.toString(),descriptor.getPropertyType());
					}
        			descriptor.getWriteMethod().invoke(bean, object);
        		}
        	}
        	return bean;
		} catch (Exception e) {
			logger.error("bean copy error for:class:{},property:{}", clazz.getName(),propertyName);
			e.printStackTrace();
			throw new RuntimeException(e);
		}
    }

    public static Map<String, Object> beanToMap(Object bean) {
    	return beanToMap(bean, false,false);
    }
    
    public static Map<String, Object> beanToMap(Object bean,boolean recursive) {
    	return beanToMap(bean, recursive,false);
    }
    
    public static Map<String, Object> beanToMap(Object bean,boolean recursive,boolean dateFormat) {
        Map<String, Object> returnMap = new HashMap<String, Object>();
        try {
            Map<String, PropertyDescriptor> descriptors = getCachePropertyDescriptors(bean.getClass());
            for (PropertyDescriptor descriptor : descriptors.values()) {
                String propertyName = descriptor.getName();
                if(CLASS_PROP_NAME.equalsIgnoreCase(propertyName))continue;
                Method readMethod = descriptor.getReadMethod();
                Object result = readMethod.invoke(bean, new Object[0]);
                if (result != null) {
                	String className = result.getClass().getName();
                	if(dynaProxyClassKeys.stream().anyMatch(o -> className.contains(o))) {
                		continue;
                	}
                	if(dateFormat && descriptor.getPropertyType() == Date.class) {
                		Field field = CachingFieldUtils.getField(bean.getClass(), propertyName);
                		if(field == null)continue;
                		if(field.isAnnotationPresent(JsonFormat.class)) {
                			JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
                			result = DateUtils.format((Date)result, jsonFormat.pattern());
                		}else if(field.isAnnotationPresent(JsonSerialize.class)) {
                			Class<?> jsonSerializer = field.getAnnotation(JsonSerialize.class).using();
                			if(jsonSerializer == DateConvertSerializer.class) {
                				result = DateUtils.format2DateStr((Date)result);
                			}
                		}else {
                			result = DateUtils.format((Date)result);
                		}
                		returnMap.put(propertyName, result);
                	}else if(isSimpleDataType(result) || result instanceof Iterable){                		
                		returnMap.put(propertyName, result);
                	}else {
                		if(recursive){
                    		returnMap.put(propertyName, beanToMap(result,recursive,dateFormat));
                    	}else{
                    		returnMap.put(propertyName,result);
                    	}
                	}
                }
            }
        } catch (Exception e) {
        	e.printStackTrace();
            throw new RuntimeException(e);
        }


        return returnMap;

    }
    
    /**
     * 拷贝map的值到对象
     * @param src
     * @param dist
     */
    public static void copy(Map<String, Object> src,Object dist){
    	if(src == null || src.isEmpty() || dist == null)return;
    	try {
    		Map<String, PropertyDescriptor> descriptors = getCachePropertyDescriptors(dist.getClass());
            for (PropertyDescriptor descriptor : descriptors.values()) {
                String propertyName = descriptor.getName();
                if(CLASS_PROP_NAME.equalsIgnoreCase(propertyName))continue;
                if(!src.containsKey(propertyName))continue;
                Object value = src.get(propertyName);
                if(value == null)continue;
                value = toAdaptTypeValue(value.getClass(), value, descriptor.getPropertyType());
                descriptor.getWriteMethod().invoke(dist, value);
            }
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }

    private static  Map<String, PropertyDescriptor> getCachePropertyDescriptors(Class<?> clazz) throws IntrospectionException {
        String canonicalName = clazz.getCanonicalName();
        Map<String, PropertyDescriptor> map = cache.get(canonicalName);

        if (map == null) {
            map = doCacheClass(clazz, canonicalName);
        }

        return map;
    }

	/**
	 * @param clazz
	 * @param canonicalName
	 * @return
	 * @throws IntrospectionException
	 */
	private synchronized static Map<String, PropertyDescriptor> doCacheClass(Class<?> clazz, String canonicalName)
			throws IntrospectionException {
		if(cache.containsKey(canonicalName))return cache.get(canonicalName);
		
		Map<String, PropertyDescriptor> map = new ConcurrentHashMap<>();
		
		List<String> fieldNames = new ArrayList<>();

		BeanInfo srcBeanInfo = Introspector.getBeanInfo(clazz);

		PropertyDescriptor[] descriptors = srcBeanInfo.getPropertyDescriptors();
		for (PropertyDescriptor descriptor : descriptors) {
			
			if("class".equals(descriptor.getName()))continue;
			if("serialVersionUID".equals(descriptor.getName()))continue;
			
			fieldNames.add(descriptor.getName());
			
		    Method readMethod = descriptor.getReadMethod();
		    Method writeMethod = descriptor.getWriteMethod();

		    String name = descriptor.getName();

		    if (readMethod == null)
		        try {
		            readMethod = clazz.getMethod("get" + name.substring(0, 1).toUpperCase() + name.substring(1));

		            descriptor.setReadMethod(readMethod);
		        } catch (NoSuchMethodException | SecurityException e) {
		        }

		    if (writeMethod == null)
		        try {
		            writeMethod = clazz.getMethod("set" + name.substring(0, 1).toUpperCase() + name.substring(1), descriptor.getPropertyType());

		            descriptor.setWriteMethod(writeMethod);
		        } catch (NoSuchMethodException | SecurityException e) {
		        }

		    if (readMethod != null && writeMethod != null) {
		        map.put(descriptor.getName(), descriptor);
		    }
		}

		cache.put(canonicalName, map);
		fieldCache.put(canonicalName, fieldNames);
		return map;
	}
	
	
	public static Class<?> getFieldGenericType(Field field){
		try {
			ParameterizedType parameterizedType = (ParameterizedType)field.getGenericType();
			Type type = parameterizedType.getActualTypeArguments()[0];
			return (Class<?>) type;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
    
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T buildMockData(Class<T> clazz) {
		try {
			T instance = clazz.newInstance();
			
			List<Field> fields = FieldUtils.getAllFieldsList(clazz);
			for (Field field : fields) {
				field.setAccessible(true);
				if (field.getType() == String.class) {
					field.set(instance, "xxxx");
				}else if (field.getType() == BigDecimal.class) {
					field.set(instance, new BigDecimal(100));
				} else if (field.getType() == byte.class || field.getType() == Byte.class) {
					field.set(instance, (byte)1);
				} else if (field.getType() == short.class || field.getType() == Short.class) {
					field.set(instance, (short)1);
				} else if (field.getType() == int.class || field.getType() == Integer.class) {
					field.set(instance, 1);
				} else if (field.getType() == double.class || field.getType() == Double.class) {
					field.set(instance, (double)1);
		        } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
		        	field.set(instance, true);
		        } else if (field.getType() == Date.class) {
		        	field.set(instance, new Date());
		        }else if(field.getType() == List.class){
		        	Class<?> fieldGenericType = getFieldGenericType(field);
		        	List list = new ArrayList<>();
		        	for (int i = 0; i < 2; i++) {
		        		list.add(buildMockData(fieldGenericType));
					}
		        	field.set(instance, list);
		        }
			}
			
			return instance;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
    
    /**
     * 判断是否基本类型
     * @param clazz
     * @return
     * @throws Exception
     */
   public static boolean isSimpleDataType(Object o) {  
	   if(o == null)return true;
       return isSimpleDataType(o.getClass()); 
   }
   
   public static boolean isSimpleDataType(Class<?> clazz) {   
       return 
       (   
    	   clazz.isPrimitive() ||
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
           clazz.equals(Date.class)   
       );   
   }
   
   /**
    * 获取泛型类
    * @param objectClass
    * @param fieldName
    * @return
    */
	private static Class<?> getGenericType(Class<?> objectClass, String fieldName) {
		try {
			Field field = FieldUtils.getField(objectClass, fieldName, true);
			Type genericType = field.getGenericType();
			if (null == genericType) {
				return null;
			}

			if (genericType instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) genericType;
				// 得到泛型里的class类型对象
				Class<?> actualTypeArgument = (Class<?>) pt.getActualTypeArguments()[0];
				return actualTypeArgument;
			}

		} catch (Exception e) {
			logger.debug("getGenericType error for class:{},property:{},error:{}",objectClass.getName(),fieldName,ExceptionFormatUtils.buildExceptionMessages(e));
		}
		return null;

	}

}
