package com.jeesuite.confcenter;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.jeesuite.confcenter.utils.HttpUtils;


public class ConfigcenterContext {

	private static ConfigcenterContext instance = new ConfigcenterContext();
	
	private boolean remoteEnabled = true;

	private String apiBaseUrl;
	private String app;
	private String env;
	private String version = "1.0.0";
	
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
		Map<String, String> params = new HashMap<>();
		try {	
			String url = String.format("%s/admin/api/fetch_all_configs?appName=%s&env=%s&version=%s", apiBaseUrl,app,env,version);
			String content = HttpUtils.getContent(url);
			System.out.println(content);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
