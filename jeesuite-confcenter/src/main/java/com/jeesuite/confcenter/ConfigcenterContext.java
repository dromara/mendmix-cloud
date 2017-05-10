package com.jeesuite.confcenter;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.confcenter.utils.HttpUtils;


public class ConfigcenterContext {

	private static ConfigcenterContext instance = new ConfigcenterContext();
	
	private boolean remoteEnabled = true;

	private String apiBaseUrl;
	private String app;
	private String env;
	private String version = "0.0.0";
	
	public static ConfigcenterContext getInstance() {
		return instance;
	}
	
	public boolean isRemoteEnabled() {
		return remoteEnabled;
	}
	public void setRemoteEnabled(boolean remoteEnabled) {
		this.remoteEnabled = remoteEnabled;
	}
	public String getApiBaseUrl() {
		return apiBaseUrl;
	}
	public void setApiBaseUrl(String apiBaseUrl) {
		this.apiBaseUrl = apiBaseUrl;
	}
	public String getApp() {
		return app;
	}
	public void setApp(String app) {
		this.app = app;
	}
	public String getEnv() {
		return env;
	}
	public void setEnv(String env) {
		this.env = env;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}

	
	public Properties getAllRemoteProperties(){
		if(!remoteEnabled)return null;
		Properties properties = new Properties();
		try {	
			String url = String.format("%s/admin/api/fetch_all_configs?appName=%s&env=%s&version=%s", apiBaseUrl,app,env,version);
			String jsonString = HttpUtils.getContent(url);
			Map<String,Object> map = JsonUtils.toObject(jsonString, Map.class);
			if(map.containsKey("code")){
				throw new RuntimeException(map.get("msg").toString());
			}
			Set<String> keys = map.keySet();
			for (String key : keys) {
				properties.put(key, map.get(key));
				//
				ResourceUtils.add(key, map.get(key).toString());  
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return properties;
	}

}
