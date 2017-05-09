package com.jeesuite.confcenter.springboot;

import java.io.IOException;
import java.util.Properties;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.jeesuite.confcenter.ConfigcenterContext;

public class CCPropertySourceLoader implements PropertySourceLoader,PriorityOrdered {

	private ConfigcenterContext ccContext = ConfigcenterContext.getInstance();
	private String activeProfile;
	@Override
	public String[] getFileExtensions() {
		return new String[] { "properties"};
	}

	@Override
	public PropertySource<?> load(String name, Resource resource, String profile)
			throws IOException {
		if (profile == null) {
			Properties properties = PropertiesLoaderUtils.loadProperties(resource);
			if(ccContext.getEnv() == null){
				ccContext.setApp(properties.getProperty("spring.application.name"));
			}
			if(activeProfile == null){
				activeProfile = properties.getProperty("spring.profiles.active");
				ccContext.setEnv(activeProfile);
			}else{
				if(name.contains(activeProfile)){
					String baseUrl = properties.getProperty("jeesuite.configcenter.base.url");
					ccContext.setApiBaseUrl(baseUrl);
					
					Properties remoteProperties = ccContext.getAllRemoteProperties();
					if(remoteProperties != null && !remoteProperties.isEmpty()){
						properties.putAll(remoteProperties);
					}
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
