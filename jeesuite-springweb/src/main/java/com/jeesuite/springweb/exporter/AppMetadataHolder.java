/*
 * Copyright 2016-2020 www.jeesuite.com.
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
package com.jeesuite.springweb.exporter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.annotation.ApiMetadata;
import com.jeesuite.common.constants.PermissionLevel;
import com.jeesuite.common.model.ApiInfo;
import com.jeesuite.common.model.Page;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.springweb.AppConfigs;
import com.jeesuite.springweb.model.AppMetadata;
import com.jeesuite.springweb.utils.UserMockUtils;


/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年4月17日
 */
public class AppMetadataHolder {

	private static AppMetadata metadata;

	private synchronized static void scanApiInfos(AppMetadata metadata,List<String> classNameList) {

		if (!metadata.getApis().isEmpty())
			return;
		
		String pathPrefix;
		if(UserMockUtils.isEnabled()) {
			pathPrefix = ResourceUtils.getProperty("jeesuite.request.pathPrefix", "");
		}else {
			pathPrefix = "";
		}
		
		Method[] methods;
		String baseUri;
		ApiInfo apiInfo;
		ApiMetadata classMetadata;
		ApiMetadata methodMetadata;
		for (String className : classNameList) {
			//if(!className.contains(GlobalRuntimeContext.MODULE_NAME))continue;
			try {
				Class<?> clazz = Class.forName(className);
				if (!clazz.isAnnotationPresent(Controller.class) && !clazz.isAnnotationPresent(RestController.class))continue;

				RequestMapping requestMapping = AnnotationUtils.findAnnotation(clazz, RequestMapping.class);
				if (requestMapping != null) {
					baseUri = requestMapping.value()[0];
					if (!baseUri.startsWith("/"))
						baseUri = "/" + baseUri;
					if (baseUri.endsWith("/"))
						baseUri = baseUri.substring(0, baseUri.length() - 1);
				} else {
					baseUri = "";
				}
				//
				classMetadata = clazz.getAnnotation(ApiMetadata.class);
				methods = clazz.getDeclaredMethods();
				Map<String, Method> interfaceMethods = getInterfaceMethods(clazz);
				methodLoop: for (Method method : methods) {
					methodMetadata = method.isAnnotationPresent(ApiMetadata.class)
							? method.getAnnotation(ApiMetadata.class) : classMetadata;
					String apiUri = null;
					String apiHttpMethod = null;
					
					requestMapping = getAnnotation(method, interfaceMethods.get(method.getName()), RequestMapping.class);
					if(requestMapping != null){
						apiUri = requestMapping.value()[0];
						if (requestMapping.method() != null && requestMapping.method().length > 0) {
							apiHttpMethod = requestMapping.method()[0].name();
						}
					}else{
						PostMapping postMapping = getAnnotation(method, interfaceMethods.get(method.getName()), PostMapping.class);
						if(postMapping != null){
							apiUri = postMapping.value().length > 0 ? postMapping.value()[0] : postMapping.path()[0];
							apiHttpMethod = RequestMethod.POST.name();
						}
						GetMapping getMapping = getAnnotation(method, interfaceMethods.get(method.getName()), GetMapping.class);
						if(getMapping != null){
							apiUri = getMapping.value().length > 0 ? getMapping.value()[0] : getMapping.path()[0];
							apiHttpMethod = RequestMethod.GET.name();
						}
					}
					
					if(StringUtils.isBlank(apiUri)){
						continue methodLoop;
					}

					apiInfo = new ApiInfo();
					if (apiUri == null) {
						apiUri = baseUri;
					} else {
						if (!apiUri.startsWith("/")) {
							apiUri = "/" + apiUri;
						}
						apiUri = baseUri + apiUri;
					}
					apiInfo.setUrl(pathPrefix + apiUri);
					apiInfo.setMethod(apiHttpMethod);
					if(methodMetadata == null){
						apiInfo.setPermissionType(PermissionLevel.LoginRequired);
					}else{								
						apiInfo.setPermissionType(methodMetadata.permissionLevel());
						apiInfo.setName(methodMetadata.actionName());
						apiInfo.setActionLog(methodMetadata.actionLog());
						apiInfo.setRequestLog(methodMetadata.requestLog());
						apiInfo.setResponseLog(methodMetadata.responseLog());
						apiInfo.setOpenApi(methodMetadata.openApi());
					}
					
					if(StringUtils.isBlank(apiInfo.getName())) {
						String name = apiInfo.getUrl().replace("/", "_");
						if(name.startsWith("_"))name = name.substring(1);
						apiInfo.setName(name);
					}
					//分页查询
					if(method.getReturnType() == Page.class) {
						apiInfo.setResponseLog(false);
					}
					//上传
					Class<?>[] parameterTypes = method.getParameterTypes();
					for (Class<?> type : parameterTypes) {
						if(type == MultipartFile.class || type == MultipartFile[].class) {
							apiInfo.setRequestLog(false);
							break;
						}
					}
					metadata.addApi(apiInfo);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("error className:" + className);
			}
		}
	}

	private static Map<String, Method> getInterfaceMethods(Class<?> clazz){
		Map<String, Method> map = new HashMap<>();
		Class<?>[] interfaces = clazz.getInterfaces();
		if(interfaces == null)return map;
		for (Class<?> class1 : interfaces) {
			Method[] methods = class1.getDeclaredMethods();
			for (Method method : methods) {				
				map.put(method.getName(), method);
			}
		}
		
		return map;
	}
	
	private static <T extends Annotation> T getAnnotation(Method classMethod,Method interfaceMethod,Class<T> annotationClass){
		T annotation = classMethod.getAnnotation(annotationClass);
		if(annotation == null && interfaceMethod != null){
			annotation = interfaceMethod.getAnnotation(annotationClass);
		}
		
		return annotation;
	}
	
	
	private static List<String> scanControllerClassNames(String basePackage){
		
		List<String> result = new ArrayList<>();
		
    	String RESOURCE_PATTERN = "/**/*.class";
    	
    	ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    	try {
            String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + ClassUtils.convertClassNameToResourcePath(basePackage) + RESOURCE_PATTERN;
            org.springframework.core.io.Resource[] resources = resourcePatternResolver.getResources(pattern);
            MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
            for (org.springframework.core.io.Resource resource : resources) {
                if (resource.isReadable()) {
                    MetadataReader reader = readerFactory.getMetadataReader(resource);
                    String className = reader.getClassMetadata().getClassName();
                    Class<?> clazz = Class.forName(className);
                    if(clazz.isAnnotationPresent(Controller.class) || clazz.isAnnotationPresent(RestController.class)){
                    	result.add(clazz.getName());
                    }
                }
            }
        } catch (Exception e) {}
    	
    	return result;
    	
	}  

	public static AppMetadata getMetadata() {
		if(metadata != null)return metadata;
		synchronized (AppMetadataHolder.class) {
			//
			metadata = new AppMetadata();
			metadata.setModule(GlobalRuntimeContext.MODULE_NAME);
			metadata.setServiceId(GlobalRuntimeContext.APPID);
			
			String basePackage = AppConfigs.basePackage;
			if(basePackage == null)return metadata;
			List<String> classNameList = scanControllerClassNames(basePackage);
			//
			scanApiInfos(metadata,classNameList);
			
			if(ResourceUtils.containsProperty("dependency.services")){				
				metadata.setDependencyServices(ResourceUtils.getList("dependency.services"));
			}
			
		}
		return metadata;
	}
	
	
	private static void addSingleClassName(List<String> result, String className) {
		try {
			Class.forName(className);
			result.add(className);
		} catch (ClassNotFoundException e) {}
	}

}
