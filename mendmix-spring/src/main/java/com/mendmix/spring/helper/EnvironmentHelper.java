/*
 * Copyright 2016-2020 www.mendmix.com.
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
package com.mendmix.spring.helper;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.util.ResourceUtils;

public class EnvironmentHelper {
	
	private static Environment environment;
	
	public static synchronized void init(ApplicationContext applicationContext) {
		if(EnvironmentHelper.environment != null)return;
		environment = applicationContext.getEnvironment();
		if(environment == null)environment = applicationContext.getBean(Environment.class);
		if(environment != null) {
			//先加载本地配置
			ResourceUtils.getAllProperties();
			//合并配置
			mergeEnvironmentProperties();
			//
			String nodeId = GlobalRuntimeContext.getNodeName();
			String workId = String.valueOf(GlobalRuntimeContext.getWorkId());
			System.setProperty("application.nodeId", nodeId);
			if(!ResourceUtils.containsProperty("m"+"endmi"+"x.cryp"+"to.cryptK"+"ey")) {
				System.setProperty("m"+"endm"+"ix.cryp"+"to.cryptK"+"ey", "fd*&fCkf@dsVu5^%f");
			}
			System.setProperty("application.workId", workId);
			ResourceUtils.printAllConfigs();
		}
	}
	

	public static String getProperty(String key){
		return environment == null ? null : environment.getProperty(key);
	}

	
	public static boolean containsProperty(String key){
		return environment == null ? false : environment.containsProperty(key);
	}
	

	private static void mergeEnvironmentProperties(){
		MutablePropertySources propertySources = ((ConfigurableEnvironment)environment).getPropertySources();
		
		int count;
		for (PropertySource<?> source : propertySources) {
			if(source.getName().startsWith("servlet") || source.getName().startsWith("system")){
				continue;
			}
			if(source.getName().contains("applicationConfig: [classpath")) {
				continue;
			}
			count = 0;
			if (source instanceof EnumerablePropertySource) {
				for (String name : ((EnumerablePropertySource<?>) source) .getPropertyNames()) {
					Object value = source.getProperty(name);
					if(value != null){
						ResourceUtils.add(name, value.toString());
						count++;
					}
				}
			}
			System.out.println(">>merge PropertySource:" + source.getName() + ",nums:" + count);
		}
	}
}
