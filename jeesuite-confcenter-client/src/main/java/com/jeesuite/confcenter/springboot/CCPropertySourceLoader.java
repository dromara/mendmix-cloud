package com.jeesuite.confcenter.springboot;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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
				String appName = getValue(properties,"jeesuite.configcenter.appName");
				if(StringUtils.isBlank(appName))appName = getValue(properties,"spring.application.name");
				ccContext.setApp(appName);
				ccContext.setRemoteEnabled(Boolean.parseBoolean(getValue(properties,"jeesuite.configcenter.enabled","true")));
			}
			if(ccContext.getApp() != null){
				if(!properties.containsKey("spring.profiles.active")){
					ccContext.setEnv(getValue(properties,"jeesuite.configcenter.profile"));
					ccContext.setApiBaseUrl(getValue(properties,"jeesuite.configcenter.base.url"));
				}else{
					ccContext.setEnv(getValue(properties,"spring.profiles.active"));
				}
			}else{
				if(name.contains(ccContext.getEnv())){
					String baseUrl = getValue(properties,"jeesuite.configcenter.base.url");
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
	
	private String getValue(Properties prop,String key,String...defVal){
		String value = StringUtils.trimToNull(prop.getProperty(key));
		if(StringUtils.isNotBlank(value)){	
			if(value.startsWith("${")){
				String refKey = value.substring(2, value.length() - 1).trim();
				value = (String) prop.getProperty(refKey);
			}
		}
		if(StringUtils.isBlank(value) && defVal != null && defVal.length >0 && defVal[0] != null){
			value = defVal[0]; 
		}
		return value;
	}
	
	@Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

}
