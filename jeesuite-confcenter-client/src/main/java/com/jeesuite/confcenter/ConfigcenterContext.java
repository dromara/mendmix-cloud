package com.jeesuite.confcenter;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.jeesuite.common.crypt.RSA;
import com.jeesuite.common.http.HttpResponseEntity;
import com.jeesuite.common.http.HttpUtils;
import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.common.util.NodeNameHolder;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common.util.SimpleCryptUtils;


public class ConfigcenterContext {

	private static ConfigcenterContext instance = new ConfigcenterContext();
	
	private PrivateKey rsaPrivateKey;
	
	private static final String DES_PREFIX = "{Cipher}";

	private static final String RSA_PREFIX = "{Cipher:RSA}";
	
	private Boolean remoteEnabled;

	private String nodeId = NodeNameHolder.getNodeId();
	private String apiBaseUrl;
	private String app;
	private String env;
	private String version;
	private String secret;
	private boolean remoteFirst = false;
	private boolean isSpringboot;
	private boolean serverInfoSynced;
	private ScheduledExecutorService hbScheduledExecutor;
	
	private ConfigcenterContext() {}

	public void init(boolean isSpringboot) {
		this.isSpringboot = isSpringboot;
		String defaultAppName = getValue("spring.application.name");
		app = getValue("jeesuite.configcenter.appName",defaultAppName);
		if(remoteEnabled == null)remoteEnabled = Boolean.parseBoolean(getValue("jeesuite.configcenter.enabled","true"));
		
		String defaultEnv = getValue("spring.profiles.active");
		env = getValue("jeesuite.configcenter.profile",defaultEnv);
		
		setApiBaseUrl(getValue("jeesuite.configcenter.base.url"));
		
		version = getValue("jeesuite.configcenter.version","0.0.0");
		
		System.out.println(String.format("\n=====configcenter=====\nappName:%s\nenv:%s\nversion:%s\nremoteEnabled:%s\n=====configcenter=====", app,env,version,remoteEnabled));
		
		String location = StringUtils.trimToNull(ResourceUtils.getProperty("jeesuite.configcenter.encrypt-keyStore-location"));
		String storeType = ResourceUtils.getProperty("jeesuite.configcenter.encrypt-keyStore-type", "JCEKS");
		String storePass = ResourceUtils.getProperty("jeesuite.configcenter.encrypt-keyStore-password");
		String alias = ResourceUtils.getProperty("jeesuite.configcenter.encrypt-keyStore-alias");
		String keyPass = ResourceUtils.getProperty("jeesuite.configcenter.encrypt-keyStore-keyPassword", storePass);
		try {			
			if(StringUtils.isNotBlank(location)){
				if(location.toLowerCase().startsWith("classpath")){
					Resource resource = new ClassPathResource(location.substring(location.indexOf(":") + 1));
					location = resource.getFile().getAbsolutePath();
				}
				rsaPrivateKey = RSA.loadPrivateKeyFromKeyStore(location, alias, storeType, storePass, keyPass);
			}
		} catch (Exception e) {
			System.out.println("load private key:"+location);
			e.printStackTrace();
		}
		
		hbScheduledExecutor = Executors.newScheduledThreadPool(1);
		
		final String url = apiBaseUrl + "/api/sync_status";
		final Map<String, String> params = new  HashMap<>();
		params.put("nodeId", nodeId);
		params.put("appName", app);
		params.put("env", env);
		hbScheduledExecutor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				//由于初始化的时候还拿不到spring.cloud.client.ipAddress，故在同步过程上送
				if(isSpringboot && serverInfoSynced == false){
					String serverip = ResourceUtils.getProperty("spring.cloud.client.ipAddress");
					if(StringUtils.isNotBlank(serverip)){						
						params.put("serverip", serverip);
						serverInfoSynced = true;
					}
				}
				
				HttpUtils.postJson(url, JsonUtils.toJson(params),HttpUtils.DEFAULT_CHARSET);
			}
		}, 30, 90, TimeUnit.SECONDS);
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
	
	public boolean isRemoteFirst() {
		return remoteFirst;
	}

	@SuppressWarnings("unchecked")
	public Properties getAllRemoteProperties(){
		if(!remoteEnabled)return null;
		if(StringUtils.isBlank(apiBaseUrl))return null;
		Properties properties = new Properties();
		
		String url = String.format("%s/api/fetch_all_configs?appName=%s&env=%s&version=%s", apiBaseUrl,app,env,version);
		System.out.println("fetch configs url:" + url);
		
		String jsonString = null;
		HttpResponseEntity response = HttpUtils.get(url);
		if(!response.isSuccessed()){
			try {Thread.sleep(1000);} catch (Exception e2) {}
			//重试一次
			jsonString = HttpUtils.get(url).getBody();
		}
		
		if(!response.isSuccessed()){
			throw new RuntimeException(response.getException());
		}
		
		jsonString = response.getBody();
		
		Map<String,Object> map = JsonUtils.toObject(jsonString, Map.class);
		if(map.containsKey("code")){
			throw new RuntimeException(map.get("msg").toString());
		}
		
		//DES解密密匙
		secret =  Objects.toString(map.remove("jeesuite.configcenter.encrypt-secret"),null);
		remoteFirst = Boolean.parseBoolean(Objects.toString(map.remove("jeesuite.configcenter.remote-config-first"),"false"));
		
		Set<String> keys = map.keySet();
		for (String key : keys) {
			Object value = decodeEncryptIfRequire(map.get(key));
			properties.put(key, value);
		}
	
		return properties;
	}
	
	public void onLoadFinish(Properties properties){
		List<String> sortKeys = new ArrayList<>();

		Map<String, String> params = new  HashMap<>();
		
		params.put("nodeId", nodeId);
		params.put("appName", app);
		params.put("env", env);
		params.put("version", version);
		params.put("springboot", String.valueOf(isSpringboot));
		if(properties.containsKey("server.port")){
			params.put("serverport", properties.getProperty("server.port"));
		}
		
		Set<Entry<Object, Object>> entrySet = properties.entrySet();
		for (Entry<Object, Object> entry : entrySet) {
			String key = entry.getKey().toString();
			String value = entry.getValue().toString();
			//如果远程配置了占位符，希望引用本地变量
			if(value.contains("${")){
				value = setReplaceHolderRefValue(properties,key,value);
			}
			
			params.put(key, hideSensitive(key, value));
			sortKeys.add(key);
		}
		
		Collections.sort(sortKeys);
		
		System.out.println("==================final config list start==================");
        for (String key : sortKeys) {
			System.out.println(String.format("%s = %s", key,params.get(key) ));
		}
		System.out.println("==================final config list end====================");
		
		if(!remoteEnabled)return;
		
		String url = apiBaseUrl + "/api/notify_final_config";
		HttpUtils.postJson(url, JsonUtils.toJson(params),HttpUtils.DEFAULT_CHARSET);
		
	}
	
	public void close(){
		hbScheduledExecutor.shutdown();
	}
	
    private String setReplaceHolderRefValue(Properties properties, String key, String value) {
		
		String[] segments = value.split("\\$\\{");
		String seg;
		
		StringBuilder finalValue = new StringBuilder();
		for (int i = 0; i < segments.length; i++) {
			seg = StringUtils.trimToNull(segments[i]);
			if(StringUtils.isBlank(seg))continue;
			
			if(seg.contains("}")){				
				String refKey = seg.substring(0, seg.indexOf("}")).trim();
				
				String refValue = properties.containsKey(refKey) ? properties.getProperty(refKey) : "${" + refKey + "}";
				finalValue.append(refValue);
				
				String[] segments2 = seg.split("\\}");
				if(segments2.length == 2){
					finalValue.append(segments2[1]);
				}
			}else{
				finalValue.append(seg);
			}
		}
		
		properties.put(key, finalValue.toString());
		
		return finalValue.toString();
	}

	private Object decodeEncryptIfRequire(Object data) {
		if (data.toString().startsWith(RSA_PREFIX)) {
			if(rsaPrivateKey == null){
				throw new RuntimeException("configcenter [rsaPrivateKey] is required");
			}
			data = data.toString().replace(RSA_PREFIX, "");
			return RSA.decrypt(rsaPrivateKey, data.toString());
		} else if (data.toString().startsWith(DES_PREFIX)) {
			if(StringUtils.isBlank(secret)){
				throw new RuntimeException("configcenter [jeesuite.configcenter.encrypt-secret] is required");
			}
			data = data.toString().replace(DES_PREFIX, "");
			return SimpleCryptUtils.decrypt(secret, data.toString());
		}
		return data;
	}

	List<String> sensitiveKeys = new ArrayList<>(Arrays.asList("pass","key","secret"));
	private String hideSensitive(String key,String orign){
		boolean is = false;
		for (String k : sensitiveKeys) {
			if(is = key.toLowerCase().contains(k))break;
		}
		int length = orign.length();
		if(is && length > 1)return orign.substring(0, length/2).concat("****");
		return orign;
	}
	
	private String getValue(String key){
		return getValue(key,null);
	}
	private String getValue(String key,String defVal){
		String value = StringUtils.trimToNull(ResourceUtils.getProperty(key,defVal));
		if(StringUtils.isNotBlank(value)){	
			if(value.startsWith("${")){
				String refKey = value.substring(2, value.length() - 1).trim();
				value = ResourceUtils.getProperty(refKey);
			}
		}
		return value;
	}
}
