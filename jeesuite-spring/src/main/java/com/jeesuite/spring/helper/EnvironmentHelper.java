package com.jeesuite.spring.helper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import com.jeesuite.spring.InstanceFactory;

public class EnvironmentHelper {

	private static Environment environment;
	
	public static String getProperty(String key){
		init();
		return environment == null ? null : environment.getProperty(key);
	}

	public static void init() {
		if(environment == null && InstanceFactory.isInitialized()){
			synchronized (EnvironmentHelper.class) {
				environment = InstanceFactory.getInstance(Environment.class);
			}
		}
	}
	
	public static boolean containsProperty(String key){
		init();
		return environment == null ? false : environment.containsProperty(key);
	}
	
	
	public static Map<String, Object> getAllProperties(String prefix){
		init();
		if(environment == null)return null;
		MutablePropertySources propertySources = ((ConfigurableEnvironment)environment).getPropertySources();
		
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		for (PropertySource<?> source : propertySources) {
			if(source.getName().startsWith("servlet") || source.getName().startsWith("system")){
				continue;
			}
			if (source instanceof EnumerablePropertySource) {
				for (String name : ((EnumerablePropertySource<?>) source) .getPropertyNames()) {
					boolean match = StringUtils.isEmpty(prefix);
					if(!match){
						match = name.startsWith(prefix);
					}
					if(match){						
						Object value = source.getProperty(name);
						if(value != null){
							properties.put(name, value);
						}
					}
				}
			}
		}
		return Collections.unmodifiableMap(properties);
	}
}
