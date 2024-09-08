/**
 * 
 */
package org.dromara.mendmix.common.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.dromara.mendmix.common.model.WrapperResponse;

/**
 * <br>
 * @author vakinge
 * @date 2023年7月27日
 */
public class MethodParseUtils {

	public static Class<?> getGenericReturnType(Method method) {
		Class<?> clazz = method.getReturnType();
		Type type = method.getGenericReturnType();
		clazz = getActualClassType(clazz, type);
		return clazz;
	}
	
	public static Class<?> getActualClassType(Class<?> clazz,Type genericType){
		if(clazz == genericType)return clazz;
		if (genericType instanceof ParameterizedType) {
			Type[] tempTypes = ((ParameterizedType) genericType).getActualTypeArguments();
			if (tempTypes[0] instanceof ParameterizedType) {
				tempTypes = ((ParameterizedType) tempTypes[0]).getActualTypeArguments();
			}
			clazz = (Class<?>) tempTypes[0];
		}
		return clazz;
	}
	
	public static Class<?> getUnWrapperClassType(Class<?> clazz,Type genericType){
		if(clazz != WrapperResponse.class)return clazz;
		if (genericType instanceof ParameterizedType) {
			Type[] tempTypes = ((ParameterizedType) genericType).getActualTypeArguments();
			Type rawType;
			if (tempTypes[0] instanceof ParameterizedType) {
				rawType = ((ParameterizedType) tempTypes[0]).getRawType();
			}else {
				rawType = tempTypes[0];
			}
			try {				
				clazz = (Class<?>) rawType;
			} catch (Exception e) {
				System.err.println("parse[" + clazz.getName() + "] error:" + e.getMessage());
			}
		}
		return clazz;
	}
	
	public static List<Parameter> getParametersWithAnnotation(Method method,Class<? extends Annotation> annotationCls) {
		Parameter[] parameters = method.getParameters();
		List<Parameter> result = new ArrayList<>(parameters.length);
        for (Parameter parameter : parameters) {
			if(parameter.isAnnotationPresent(annotationCls)) {
				result.add(parameter);
			}
		}
		return result;
	}
	
}
