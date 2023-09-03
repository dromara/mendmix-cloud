/**
 * 
 */
package com.mendmix.springweb.exporter.apidoc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.mendmix.common.constants.ValueType;
import com.mendmix.common.model.ApiInfo;
import com.mendmix.common.model.OrderBy;
import com.mendmix.common.model.Page;
import com.mendmix.common.util.BeanUtils;

/**
 * <br>
 * @author 姜维(00770874)
 * @date 2023年7月27日
 */
public class ApiDocInfo extends ApiInfo {

	private RequestMetadata requestMeta;
	private ResponseMetadata responseMeta;
	
	public static ApiDocInfo buildFrom(ApiInfo api,String pathPrefix) {
		ApiDocInfo apiDocInfo = BeanUtils.copy(api, ApiDocInfo.class);
		if(StringUtils.isNotBlank(pathPrefix)) {
			apiDocInfo.setUri(pathPrefix + api.getUri());
		}
		apiDocInfo.buildReqResMetadata();
		return apiDocInfo;
	}

	public RequestMetadata getRequestMeta() {
		return requestMeta;
	}

	public void setRequestMeta(RequestMetadata requestMeta) {
		this.requestMeta = requestMeta;
	}

	public ResponseMetadata getResponseMeta() {
		return responseMeta;
	}

	public void setResponseMeta(ResponseMetadata responseMeta) {
		this.responseMeta = responseMeta;
	}
	
	
	private void buildReqResMetadata() {
		Method method = getControllerMethod();
		//request
		this.requestMeta = new RequestMetadata();
		Parameter[] parameters = method.getParameters();
		String value;
		for (Parameter parameter : parameters) {
			if(parameter.isAnnotationPresent(PathVariable.class)) {
				PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
				value = StringUtils.defaultString(StringUtils.trimToNull(pathVariable.value()), pathVariable.name());
				this.requestMeta.addPathParameter(new SimpleParameter(value, value, true));
			}else if(parameter.isAnnotationPresent(RequestParam.class)) {
				RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
				value = StringUtils.defaultString(StringUtils.trimToNull(requestParam.value()), requestParam.name());
				this.requestMeta.addQueryParameter(new SimpleParameter(value, value, requestParam.required()));
			}else if(parameter.isAnnotationPresent(RequestBody.class)) {
				CompositeParameter body = buildCompositeParameter(null,parameter.getType(), parameter.getParameterizedType());
				this.requestMeta.setBody(body);
			}
		}
		//response
		Class<?> returnClass = method.getReturnType();
		CompositeParameter body = buildCompositeParameter(null,returnClass, method.getGenericReturnType());
		this.responseMeta = new ResponseMetadata(body, true);
		
	}
	
	private CompositeParameter buildCompositeParameter(String parameterKey,Class<?> returnClass,Type genericType) {
		CompositeParameter parameter = null;
		if(returnClass == void.class || BeanUtils.isSimpleDataType(returnClass)) {
			parameter = new CompositeParameter(parameterKey,ValueType.string);
		}else if(Iterable.class.isAssignableFrom(returnClass)) {
			parameter = new CompositeParameter(parameterKey,ValueType.array);
			Class<?> typeClass = getActualClass(genericType);
			CompositeParameter arrayItemParameter = buildCompositeParameter(null,typeClass, typeClass);
			parameter.setArrayItems(arrayItemParameter);
		}else {
			parameter = new CompositeParameter(parameterKey,ValueType.object);
			Field[] fields = FieldUtils.getAllFields(returnClass);
			CompositeParameter child = null;
			Class<?> typeClass;
			for (Field field : fields) {
				typeClass = field.getType();
				if(typeClass == OrderBy.class)continue;
				if(BeanUtils.isSimpleDataType(field.getType())) {
					child = new CompositeParameter(field.getName(),typeClass);
				}else if(Iterable.class.isAssignableFrom(typeClass)) {
					child = new CompositeParameter(field.getName(),ValueType.array);
					//TODO 判断泛型
					if(returnClass == Page.class) {
						typeClass = getActualClass(genericType);
					}else {
						typeClass = getActualClass(field.getGenericType());
					}
					CompositeParameter arrayItemParameter = buildCompositeParameter(field.getName(),typeClass, typeClass);
					child.setArrayItems(arrayItemParameter);
				}else if(typeClass != field.getGenericType()) { //泛型
					typeClass = getActualClass(genericType);
					child = buildCompositeParameter(field.getName(),typeClass, typeClass);
				}else {
					child = buildCompositeParameter(field.getName(),field.getType(), field.getGenericType());
				}
				if(child != null) {
					parameter.addChild(child);
				}
			}
		}
		
		return parameter;
	}
	
	private Class<?> getActualClass(Type genericType){
		if (genericType instanceof ParameterizedType) {
		   Type[] tempTypes = ((ParameterizedType) genericType).getActualTypeArguments();
		   return (Class<?>) tempTypes[0];
		}
		return null;
	}

}
