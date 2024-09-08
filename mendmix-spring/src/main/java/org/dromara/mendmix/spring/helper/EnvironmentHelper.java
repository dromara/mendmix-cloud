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
package org.dromara.mendmix.spring.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.crypt.CustomEncryptor;
import org.dromara.mendmix.common.util.ResourceUtils;

public class EnvironmentHelper {
	
	private static Environment environment;
	
	public static synchronized void init(ApplicationContext applicationContext) {
		if(EnvironmentHelper.environment != null)return;
		environment = applicationContext.getEnvironment();
		if(environment == null)environment = applicationContext.getBean(Environment.class);
		if(environment != null) {
			//合并配置
			List<String> resolvedKeys = mergeEnvironmentProperties();
			//打印配置
			ResourceUtils.printAllConfigs(resolvedKeys);
			//
			System.setProperty("client.nodeId", GlobalContext.getNodeName());
		}
	}
	

	public static String getProperty(String key){
		return environment == null ? null : environment.getProperty(key);
	}

	
	public static boolean containsProperty(String key){
		return environment == null ? false : environment.containsProperty(key);
	}
	

	private static List<String> mergeEnvironmentProperties(){
		MutablePropertySources propertySources = ((ConfigurableEnvironment)environment).getPropertySources();
		
		List<String> names = propertySources.stream().map(o -> o.getName()).collect(Collectors.toList());
		String name;
		PropertySource<?> source;
		for (int i = names.size() - 1; i >= 0; i--) {
			name = names.get(i);
			if(name.startsWith("servlet") 
					|| name.equals("systemEnvironment") 
					|| name.equals("systemProperties") 
					|| name.equals("random") 
					|| name.equals("cachedrandom")) {
				continue;
			}
			//本地文件
			if(name.contains("classpath") || name.contains("class path")) {
				continue;
			}
			source = propertySources.get(name);
			int count = 0;
			if (source instanceof EnumerablePropertySource) {
				for (String propKey : ((EnumerablePropertySource<?>) source) .getPropertyNames()) {
					Object value = source.getProperty(propKey);
					if(value != null){
						ResourceUtils.add(propKey, value.toString());
						count++;
					}
				}
			}
			System.out.println(">>merge PropertySource:" + source.getName() + ",nums:" + count);
		}
		//
		List<String> sensitiveKeys = new ArrayList<>();
		Properties resolveProperties = new Properties();
		String key;
		String value;
		Iterator<Entry<Object, Object>> iterator = System.getProperties().entrySet().iterator();
		Entry<Object, Object> entry;
		while(iterator.hasNext()) {
			entry = iterator.next();
			key = entry.getKey().toString();
			value = Objects.toString(entry.getValue(), null);
			if(value != null && value.startsWith("{hidden}")) {
				value = value.substring(8);
				resolveProperties.setProperty(key, value);
				sensitiveKeys.add(key);
				iterator.remove();
			}
		}
		//
		iterator = ResourceUtils.getAllProperties().entrySet().iterator();
		while(iterator.hasNext()) {
			entry = iterator.next();
			key = entry.getKey().toString();
			if(System.getProperties().containsKey(key)) {
				value = System.getProperty(key);
				ResourceUtils.add(key, value);
			}else if(!resolveProperties.containsKey(key)){
				value = entry.getValue().toString();
				if(value.startsWith(GlobalConstants.CRYPT_PREFIX)) {
					value = decryptConfigValue(value);
					resolveProperties.setProperty(key, value);
					sensitiveKeys.add(key);
				}
			}
		}
		//
		if(!resolveProperties.isEmpty()) {
			OriginTrackedMapPropertySource propertySource = new OriginTrackedMapPropertySource("dynaResolveProperties", resolveProperties);
			propertySources.addFirst(propertySource);
			ResourceUtils.merge(resolveProperties);
		}
		return sensitiveKeys;
	}


	/**
	 * @param value
	 * @return
	 */
	public static String decryptConfigValue(String value) {
		String plainText = value.replace(GlobalConstants.CRYPT_PREFIX, "").trim();
		plainText = CustomEncryptor.useDefault().decrypt(plainText);
		return plainText;
	}

}
