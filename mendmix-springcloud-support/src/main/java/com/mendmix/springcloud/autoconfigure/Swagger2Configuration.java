/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.springcloud.autoconfigure;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.async.DeferredResult;

import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.springweb.AppConfigs;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@ConditionalOnProperty(name = "mendmix.application.base-package")
@ConditionalOnClass(springfox.documentation.spring.web.plugins.Docket.class)
@EnableSwagger2
public class Swagger2Configuration {

	@Bean
	public Docket createRestApi() {
		
		boolean enable = GlobalRuntimeContext.ENV.equals("dev") || GlobalRuntimeContext.ENV.equals("local");
		String applicationName = ResourceUtils.getProperty("spring.application.name");
		String basePackage = AppConfigs.basePackage;
		Docket docket = new Docket(DocumentationType.SWAGGER_2)
				.apiInfo(apiInfo(applicationName)) //
				.enable(enable)  //
				.genericModelSubstitutes(DeferredResult.class) //
				.useDefaultResponseMessages(false)  //
				.forCodeGeneration(true)  //
				.select()
				.apis(RequestHandlerSelectors.basePackage(basePackage))
				.paths(PathSelectors.any())
				.build();
		
		docket.securitySchemes(securitySchemes()).securityContexts(securityContexts());
		
		return docket;
	}

	private ApiInfo apiInfo(String appName) {
		return new ApiInfoBuilder().title(appName + " APIs").description(appName + " 项目说明")
				.version(ResourceUtils.getProperty("info.app.version", "1.0.0")).build();
	}

	private List<ApiKey> securitySchemes() {
		return Arrays.asList(new ApiKey("Authorization", "Authorization", "header"));
	}

	private List<SecurityContext> securityContexts() {
		return Arrays.asList(SecurityContext.builder().securityReferences(defaultAuth())
				.forPaths(PathSelectors.any()).build());
	}
	
	List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "全局权限");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return Arrays.asList(new SecurityReference("Authorization", authorizationScopes));
    }

}
