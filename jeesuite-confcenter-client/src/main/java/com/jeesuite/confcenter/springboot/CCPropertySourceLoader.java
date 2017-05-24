package com.jeesuite.confcenter.springboot;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.jeesuite.confcenter.ConfigcenterContext;

public class CCPropertySourceLoader implements PropertySourceLoader,PriorityOrdered {

	private ConfigcenterContext ccContext = ConfigcenterContext.getInstance();
	@Override
	public String[] getFileExtensions() {
		return new String[] { "properties"};
	}

	@Override
	public PropertySource<?> load(String name, Resource resource, String profile)
			throws IOException {
		if (profile == null) {
			Properties properties = PropertiesLoaderUtils.loadProperties(resource);
			if(ccContext.getApp() == null){
				ccContext.setApp(properties.getProperty("spring.application.name"));
				
			}
			if(ccContext.getApp() != null){
				if(!properties.containsKey("spring.profiles.active")){
					ccContext.setEnv(properties.getProperty("jeesuite.configcenter.profile"));
					ccContext.setApiBaseUrl(properties.getProperty("jeesuite.configcenter.base.url"));
				}else{
					ccContext.setEnv(properties.getProperty("spring.profiles.active"));
				}
			}else{
				if(name.contains(ccContext.getEnv())){
					String baseUrl = properties.getProperty("jeesuite.configcenter.base.url");
					ccContext.setApiBaseUrl(baseUrl);
				}
			}
			
			if(ccContext.getApiBaseUrl() != null){
				Properties remoteProperties = ccContext.getAllRemoteProperties();
				if(remoteProperties != null){
					Set<Entry<Object, Object>> entrySet = remoteProperties.entrySet();
					for (Entry<Object, Object> entry : entrySet) {
						//本地配置优先原则
						if(properties.containsKey(entry.getKey()))continue;
						properties.put(entry.getKey(), entry.getValue());
					}
					//properties.putAll(remoteProperties);
				}
			}
			
			if (!properties.isEmpty()) {
				return new PropertiesPropertySource(name, properties);
			}
		}
		return null;
	}
	
	@Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

}
