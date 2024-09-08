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
package org.dromara.mendmix.springweb.exporter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.annotation.ApiMetadata;
import org.dromara.mendmix.common.constants.PermissionLevel;
import org.dromara.mendmix.common.constants.ValueType;
import org.dromara.mendmix.common.http.HttpMethod;
import org.dromara.mendmix.common.model.ApiInfo;
import org.dromara.mendmix.common.model.Page;
import org.dromara.mendmix.common.util.BeanUtils;
import org.dromara.mendmix.common.util.MethodParseUtils;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.logging.LogConfigs;
import org.dromara.mendmix.springweb.model.AppMetadata;
import org.dromara.mendmix.springweb.utils.UserMockUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.ApiOperation;


/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年4月17日
 */
public class AppMetadataHolder {

	private static Logger log = LoggerFactory.getLogger("org.dromara.mendmix.springweb");
	
	private static AppMetadata metadata;

	private synchronized static void scanApiInfos(AppMetadata metadata,String modulePrefix,List<String> classNameList) {

		String pathPrefix = GlobalContext.getContextPath();
		if(modulePrefix != null) {
			pathPrefix = pathPrefix + addFirstPathSeparator(modulePrefix);
		}

		Method[] methods;
		String baseUri;
		ApiInfo apiInfo;
		ApiMetadata classMetadata;
		ApiMetadata methodMetadata;
		for (String className : classNameList) {
			//if(!className.contains(GlobalContext.MODULE_NAME))continue;
			try {
				Class<?> clazz = Class.forName(className);
				if (!clazz.isAnnotationPresent(Controller.class) && !clazz.isAnnotationPresent(RestController.class))continue;

				RequestMapping requestMapping = AnnotationUtils.findAnnotation(clazz, RequestMapping.class);
				if (requestMapping != null) {
					baseUri = addFirstPathSeparator(requestMapping.value().length > 0 ? requestMapping.value()[0] : requestMapping.path()[0]);
					if (baseUri.endsWith("/")) {
						baseUri = baseUri.substring(0, baseUri.length() - 1);
					}
				} else {
					baseUri = "";
				}
				//
				classMetadata = clazz.getAnnotation(ApiMetadata.class);
				methods = clazz.getMethods();
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
					}
					//
					if(apiHttpMethod == null) {
						PostMapping methodMapping = getAnnotation(method, interfaceMethods.get(method.getName()), PostMapping.class);
						if(methodMapping != null){
							if(methodMapping.value().length > 0) {
								apiUri = methodMapping.value()[0];
							}else if(methodMapping.path().length > 0){
								apiUri = methodMapping.path()[0];
							}else {
								apiUri = "";
							}
							apiHttpMethod = RequestMethod.POST.name();
					   }
					}
					//
					if(apiHttpMethod == null) {
						GetMapping methodMapping = getAnnotation(method, interfaceMethods.get(method.getName()), GetMapping.class);
						if(methodMapping != null){
							if(methodMapping.value().length > 0) {
								apiUri = methodMapping.value()[0];
							}else if(methodMapping.path().length > 0){
								apiUri = methodMapping.path()[0];
							}else {
								apiUri = "";
							}
							apiHttpMethod = RequestMethod.GET.name();
						}
					}
					//
					if(apiHttpMethod == null) {
						DeleteMapping methodMapping = getAnnotation(method, interfaceMethods.get(method.getName()), DeleteMapping.class);
						if(methodMapping != null){
							if(methodMapping.value().length > 0) {
								apiUri = methodMapping.value()[0];
							}else if(methodMapping.path().length > 0){
								apiUri = methodMapping.path()[0];
							}else {
								apiUri = "";
							}
							apiHttpMethod = RequestMethod.DELETE.name();
						}
					}
					//
					if(apiHttpMethod == null) {
						PutMapping methodMapping = getAnnotation(method, interfaceMethods.get(method.getName()), PutMapping.class);
						if(methodMapping != null){
							if(methodMapping.value().length > 0) {
								apiUri = methodMapping.value()[0];
							}else if(methodMapping.path().length > 0){
								apiUri = methodMapping.path()[0];
							}else {
								apiUri = "";
							}
							apiHttpMethod = RequestMethod.PUT.name();
						}
					}
					
					if(StringUtils.isBlank(apiUri)){
						continue methodLoop;
					}

					apiInfo = new ApiInfo();
					if (apiUri == null) {
						apiUri = baseUri;
					} else {
						apiUri = baseUri + addFirstPathSeparator(apiUri);
					}
					apiInfo.setUri(pathPrefix + apiUri);
					apiInfo.setMethod(apiHttpMethod);
					apiInfo.setControllerMethod(method);
					apiInfo.setControllerMethodName(className + "." + method.getName());
					if(methodMetadata == null){
						apiInfo.setPermissionLevel(PermissionLevel.LoginRequired);
					}else{								
						apiInfo.setPermissionLevel(methodMetadata.permissionLevel());
						apiInfo.setName(methodMetadata.actionName());
						apiInfo.setApiLog(methodMetadata.apiLog());
						apiInfo.setActionLog(methodMetadata.actionLog());
						if(methodMetadata.requestLog().length > 0) {
							apiInfo.setRequestLog(methodMetadata.requestLog()[0]);
						}
						if(methodMetadata.requestLog().length > 0) {
							apiInfo.setRequestLog(methodMetadata.requestLog()[0]);
						}
						if(methodMetadata.responseLog().length > 0) {
							apiInfo.setResponseLog(methodMetadata.responseLog()[0]);
						}
						apiInfo.setOpenApi(methodMetadata.openApi());
					}
					
					if(StringUtils.isBlank(apiInfo.getName())) {
						ApiOperation apiOperation = getAnnotation(method, interfaceMethods.get(method.getName()), ApiOperation.class);
					    if(apiOperation != null) {
					    	apiInfo.setName(apiOperation.value());
					    }
					}
					
					if(StringUtils.isBlank(apiInfo.getName())) {
						String name = apiInfo.getUri().replace("/", "_");
						if(name.startsWith("_"))name = name.substring(1);
						apiInfo.setName(name);
					}
					//
					if(!GlobalContext.isGateway()) {
						Class<?> wrapperClassType = MethodParseUtils.getUnWrapperClassType(method.getReturnType(), method.getGenericReturnType());
						if(wrapperClassType == Page.class) {
							apiInfo.setReturnType(ValueType.page);
							if(LogConfigs.API_LOG_IGNORE_PAGE_QUERY_RESP_BODY) {
								apiInfo.setResponseLog(false);
							}
						}else if(Iterable.class.isAssignableFrom(wrapperClassType)) {
							apiInfo.setReturnType(ValueType.array);
						}else if(BeanUtils.isSimpleDataType(wrapperClassType)) {
							apiInfo.setReturnType(ValueType.string);
						}else {
							apiInfo.setReturnType(ValueType.object);
						}
						//上传
						Class<?>[] parameterTypes = method.getParameterTypes();
						parameterLoop:for (Class<?> type : parameterTypes) {
							if(type == MultipartFile.class || type == MultipartFile[].class) {
								apiInfo.setRequestLog(false);
								break parameterLoop;
							}
						}
					}
					//@RequestMapping 没指定method的情况
					if(apiInfo.getMethod() == null) {
						apiInfo.setMethod(HttpMethod.GET.name());
						ApiInfo tmpApiInfo = BeanUtils.copy(apiInfo, ApiInfo.class);
						tmpApiInfo.setIdentifier(null);
						tmpApiInfo.setMethod(HttpMethod.POST.name());
						metadata.addApi(tmpApiInfo);
					}
					metadata.addApi(apiInfo);
				}
			} catch (Exception e) {
				System.err.println("parse apimetadata error className:" + className + ",error:" + e.getMessage());
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
			if(metadata != null)return metadata;
			//
			AppMetadata _metadata = new AppMetadata();
			_metadata.setSystem(GlobalContext.SYSTEM_KEY);
			_metadata.setModule(GlobalContext.APPID);
			_metadata.setStdResponse(ResourceUtils.getBoolean("response.rewrite.enbaled", true));
			_metadata.setApiLogging(LogConfigs.API_LOGGING);
			
			String basePackagesPropVal = ResourceUtils.getAnyProperty("mendmix-cloud.metadata.scan.packages","mendmix-cloud.base-package");
			List<String> basePackages;
			if(basePackagesPropVal == null) {
				basePackages = new ArrayList<>();
			}else {
				basePackages = new ArrayList<>(Arrays.asList(StringUtils.split(basePackagesPropVal, ",;")));
			}
			//分包
			Map<String, String> packagePrefixs = ResourceUtils.getMappingValues("mendmix-cloud.request.pathPrefix.mapping");
			String globalPrefix = null;
			if(UserMockUtils.isEnabled() && ResourceUtils.containsProperty("local.debug.mock.pathPrefix")) {
				globalPrefix = ResourceUtils.getProperty("local.debug.mock.pathPrefix");
			}
			//
			for (String subPackage : packagePrefixs.keySet()) {
				if(!basePackages.contains(subPackage)) {
					basePackages.add(subPackage);
				}
			}
			
			log.info(">>scan apis basePackages:{}",basePackages);
			String curPathPrefix;
			List<String> classNameList;
			for (String basePackage : basePackages) {
				curPathPrefix = packagePrefixs.getOrDefault(basePackage, globalPrefix);
				classNameList = scanControllerClassNames(basePackage);
				//
				scanApiInfos(_metadata,curPathPrefix,classNameList);
			}
			//
			if(GlobalContext.isGateway()) {
				classNameList = new ArrayList<>(2);
				classNameList.addAll(scanControllerClassNames("org.dromara.mendmix.adapter.gateway.controller"));
				classNameList.addAll(scanControllerClassNames("org.dromara.mendmix.edge.exporter"));
				scanApiInfos(_metadata,globalPrefix,classNameList);
			}else {
				classNameList = new ArrayList<>(2);
				classNameList.addAll(scanControllerClassNames("org.dromara.mendmix.exporter"));
				scanApiInfos(_metadata,globalPrefix,classNameList);
			}
			
			_metadata.onInitFinished();
			metadata = _metadata;
			
		}
		return metadata;
	}
	
	private static String addFirstPathSeparator(String uri) {
		if(StringUtils.isBlank(uri))return StringUtils.EMPTY;
		if(uri.startsWith(GlobalConstants.PATH_SEPARATOR))return uri;
		return GlobalConstants.PATH_SEPARATOR + uri;
	}

	public static ApiInfo getCurrentApiInfo() {
		ApiInfo api = ThreadLocalContext.get(GlobalConstants.CONTEXT_CURRENT_API_KEY);
		if(api == null) {
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
			api = getMetadata().getApi(request.getMethod(),request.getRequestURI());
		    if(api != null) {
		    	ThreadLocalContext.set(GlobalConstants.CONTEXT_CURRENT_API_KEY, api);
		    }
		}
		return api;
	}

}
