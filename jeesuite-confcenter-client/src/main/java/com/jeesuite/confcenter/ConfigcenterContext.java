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
		if(apiBaseUrl != null)if(apiBaseUrl.endsWith("/"))apiBaseUrl = apiBaseUrl.substring(0, apiBaseUrl.length() - 1);
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

	
	@SuppressWarnings("unchecked")
	public Properties getAllRemoteProperties(){
		if(!remoteEnabled)return null;
		Properties properties = new Properties();
		try {	
			String url = String.format("%s/api/fetch_all_configs?appName=%s&env=%s&version=%s", apiBaseUrl,app,env,version);
			System.out.println("fetch configs url:" + url);
			
			String jsonString = null;
			try {
				jsonString = HttpUtils.getContent(url);
			} catch (Exception e) {
				try {Thread.sleep(500);} catch (Exception e2) {}
				//重试一次
				jsonString = HttpUtils.getContent(url);
			}
			Map<String,Object> map = JsonUtils.toObject(jsonString, Map.class);
			if(map.containsKey("code")){
				throw new RuntimeException(map.get("msg").toString());
			}
			Set<String> keys = map.keySet();
			System.out.println("==================remote config list =======================");
			for (String key : keys) {
				properties.put(key, map.get(key));
				//
				ResourceUtils.add(key, map.get(key).toString());  
				if(!key.contains("password")){					
					System.out.println(String.format("%s=%s", key,map.get(key).toString()));
				}
			}
			System.out.println("==================remote config list=======================");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return properties;
	}

}
