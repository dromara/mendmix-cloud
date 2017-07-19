package com.jeesuite.confcenter;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.crypt.RSA;
import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common.util.SimpleCryptUtils;
import com.jeesuite.confcenter.utils.HttpUtils;


public class ConfigcenterContext {

	private final static Logger logger = LoggerFactory.getLogger(ConfigcenterContext.class);
	
	private static ConfigcenterContext instance = new ConfigcenterContext();
	
	private PrivateKey rsaPrivateKey;
	
	private static final String DES_PREFIX = "{Cipher}";

	private static final String RSA_PREFIX = "{Cipher:RSA}";
	
	private boolean remoteEnabled = true;

	private String apiBaseUrl;
	private String app;
	private String env;
	private String version = "0.0.0";
	private String secret;
	
	private ConfigcenterContext() {
		secret =  ResourceUtils.get("jeesuite.configcenter.encrypt-secret");
		String location = ResourceUtils.get("jeesuite.configcenter.encrypt-keyStore-location");
		String storeType = ResourceUtils.get("jeesuite.configcenter.encrypt-keyStore-type", "JCEKS");
		String storePass = ResourceUtils.get("jeesuite.configcenter.encrypt-keyStore-password");
		String alias = ResourceUtils.get("jeesuite.configcenter.encrypt-keyStore-alias");
		String keyPass = ResourceUtils.get("jeesuite.configcenter.encrypt-keyStore-keyPassword", storePass);
		try {			
			if(StringUtils.isNotBlank(location)){
				rsaPrivateKey = RSA.loadPrivateKeyFromKeyStore(location, alias, storeType, storePass, keyPass);
			}
		} catch (Exception e) {
			logger.warn("load private key:"+location,e);
		}
	}

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
    
	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	@SuppressWarnings("unchecked")
	public Properties getAllRemoteProperties(){
		if(!remoteEnabled)return null;
		Properties properties = new Properties();
		
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
			Object value = decodeEncryptIfRequire(map.get(key));
			properties.put(key, value);
			//
			ResourceUtils.add(key, value.toString());  
			if(!key.contains("password")){					
				System.out.println(String.format("%s=%s", key,hideSensitive(key,value.toString())));
			}
		}
		System.out.println("==================remote config list=======================");
	
		return properties;
	}

	private Object decodeEncryptIfRequire(Object data) {
		if (data.toString().startsWith(RSA_PREFIX)) {
			if(rsaPrivateKey == null){
				throw new RuntimeException("configcenter [secrrsaPrivateKeyet] is required");
			}
			data = data.toString().replace(RSA_PREFIX, "");
			return RSA.decode(rsaPrivateKey, data.toString());
		} else if (data.toString().startsWith(DES_PREFIX)) {
			if(StringUtils.isBlank(secret)){
				throw new RuntimeException("configcenter [secret] is required");
			}
			data = data.toString().replace(DES_PREFIX, "");
			return SimpleCryptUtils.decode(secret, data.toString());
		}
		return data;
	}

	List<String> sensitiveKeys = new ArrayList<>(Arrays.asList("pass","key","secret"));
	private String hideSensitive(String key,String orign){
		boolean is = false;
		for (String k : sensitiveKeys) {
			if(is = key.toLowerCase().contains(k))break;
		}
		if(is)return orign.substring(0, orign.length()/2).concat("****");
		return orign;
	}
}
