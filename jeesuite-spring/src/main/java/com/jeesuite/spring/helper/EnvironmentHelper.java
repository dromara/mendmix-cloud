package com.jeesuite.spring.helper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import com.jeesuite.common.util.ResourceUtils;

public class EnvironmentHelper {
	

	private static Environment environment;
	
	public static synchronized void init(ApplicationContext context) {
		if(environment != null){
			return;
		}
		environment = context.getEnvironment();
		ResourceUtils.merge(getAllProperties(null));
		//
		if(!ResourceUtils.getBoolean("jeesuite.config.enabled", true)) {
			ResourceUtils.printConfigs(ResourceUtils.getAllProperties());
		}
	}

	public static String getProperty(String key){
		return environment == null ? null : environment.getProperty(key);
	}

	public static boolean containsProperty(String key){
		return environment == null ? false : environment.containsProperty(key);
	}
	
	
	public static Map<String, Object> getAllProperties(String prefix){
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
