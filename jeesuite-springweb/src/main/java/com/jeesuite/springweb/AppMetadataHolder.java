/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jeesuite.springweb;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.jeesuite.common.constants.PermissionLevel;
import com.jeesuite.common.util.ClassScanner;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.springweb.annotation.ApiMetadata;
import com.jeesuite.springweb.ext.feign.FeignApiDependencyScanner;
import com.jeesuite.springweb.model.ApiInfo;
import com.jeesuite.springweb.model.AppMetadata;

import io.swagger.annotations.ApiOperation;

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
		
		Method[] methods;
		String baseUri;
		ApiInfo apiInfo;
		ApiMetadata classMetadata;
		ApiMetadata methodMetadata;
		for (String className : classNameList) {
			if(!className.contains(CurrentRuntimeContext.MODULE_NAME))continue;
			try {
				Class<?> clazz = Class.forName(className);
				if (clazz.isAnnotationPresent(Controller.class)
						|| clazz.isAnnotationPresent(RestController.class)) {
					if (clazz.isAnnotationPresent(RequestMapping.class)) {
						baseUri = clazz.getAnnotation(RequestMapping.class).value()[0];
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
						
						RequestMapping requestMapping = getAnnotation(method, interfaceMethods.get(method.getName()), RequestMapping.class);
						if(requestMapping != null){
							apiUri = requestMapping.value()[0];
							if (requestMapping.method() != null && requestMapping.method().length > 0) {
								apiHttpMethod = requestMapping.method()[0].name();
							}
						}else{
							PostMapping postMapping = getAnnotation(method, interfaceMethods.get(method.getName()), PostMapping.class);
							if(postMapping != null){
								apiUri = postMapping.value()[0];
								apiHttpMethod = RequestMethod.POST.name();
							}
							GetMapping getMapping = getAnnotation(method, interfaceMethods.get(method.getName()), GetMapping.class);
							if(getMapping != null){
								apiUri = getMapping.value()[0];
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
						apiInfo.setUrl(apiUri);
						apiInfo.setMethod(apiHttpMethod);

						if (method.isAnnotationPresent(ApiOperation.class)) {
							apiInfo.setName(method.getAnnotation(ApiOperation.class).value());
						} else {
							apiInfo.setName(apiInfo.getUrl());
						}
		
						if (methodMetadata != null && StringUtils.isNotBlank(methodMetadata.actionName())) {
							apiInfo.setName(methodMetadata.actionName());
						} else if (method.isAnnotationPresent(ApiOperation.class)) {
							apiInfo.setName(method.getAnnotation(ApiOperation.class).value());
						}
						
						if(methodMetadata == null){
							apiInfo.setPermissionType(PermissionLevel.LoginRequired);
						}else{								
							apiInfo.setPermissionType(methodMetadata.permissionLevel());
						}
						metadata.getApis().add(apiInfo);
					}
				}
			} catch (Exception e) {
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

	public static AppMetadata getMetadata() {
		if(metadata != null)return metadata;
		synchronized (AppMetadataHolder.class) {
			//
			metadata = new AppMetadata();
			metadata.setAppId(CurrentRuntimeContext.APPID);
			metadata.setServiceId(CurrentRuntimeContext.SERVICE_NAME);
			if(ResourceUtils.containsProperty("dependency.services")){				
				metadata.setDependencyServices(Arrays.asList(ResourceUtils.getProperty("dependency.services")));
			}
			
			String basePackage = ResourceUtils.getProperty("application.base-package");
			if(basePackage == null)return metadata;
			List<String> classNameList = ClassScanner.scan(basePackage);
			//
			scanApiInfos(metadata,classNameList);
			
			if(ResourceUtils.containsProperty("dependency.services")){				
				metadata.setDependencyServices(Arrays.asList(ResourceUtils.getProperty("application.dependency.services")));
			}else{
				try {
					Class.forName("org.springframework.cloud.openfeign.FeignClient");
					metadata.setDependencyServices(FeignApiDependencyScanner.doScan(classNameList));
				} catch (Exception e) {}
			}
			
		}
		return metadata;
	}

}
