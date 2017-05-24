/**
 * 
 */
package com.jeesuite.confcenter.spring;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.util.StringUtils;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.confcenter.utils.ConfigZkPathUtils;
import com.jeesuite.confcenter.utils.HttpUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月2日
 */
public class CCPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer implements DisposableBean{
	
	private final static Logger logger = LoggerFactory.getLogger(CCPropertyPlaceholderConfigurer.class);

	private boolean remoteEnabled = true;
	
	private String apiBaseUrl;
	private String app;
	private String env;
	private String version;
	
	private ZkClient zkClient;
	
	public void setRemoteEnabled(boolean remoteEnabled) {
		this.remoteEnabled = remoteEnabled;
	}

	@Override
	protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props)
			throws BeansException {
		super.processProperties(beanFactoryToProcess, props);
	}



	@Override
	protected Properties mergeProperties() throws IOException {
		Properties properties = super.mergeProperties();
		if(remoteEnabled){
			//TODO 暂时不同步zk（这一块交互逻辑还没想清楚。 同步到zk通过管理界面呈现？？ 通过zk动态更新配置？？）
			//initZkClient();
			
			URL resource = Thread.currentThread().getContextClassLoader().getResource("confcenter.properties");
			if(resource == null)throw new FileNotFoundException("配置文件[confcenter.properties]缺失");
			Properties config = new Properties();
			config.load(new FileReader(new File(resource.getPath())));
			
			apiBaseUrl = config.getProperty("confcenter.url");
			if(apiBaseUrl.endsWith("/"))apiBaseUrl = apiBaseUrl.substring(0, apiBaseUrl.length() - 1);
			app = config.getProperty("app.name");
			env = config.getProperty("app.env");
			version = config.getProperty("app.version");
			
			properties.putAll(loadRemoteProperties(config));
			
			//发布所有配置ZK
			publishPropsToZK(properties);
			
		}
		return properties;
		//
	}

	/**
	 * 同步最终配置到ZK
	 * @param properties
	 */
	private void publishPropsToZK(Properties properties) {
		if(zkClient == null)return;
		Map<String, Object> props = new HashMap<>();
		for(String key : properties.stringPropertyNames()) { 
			String value = properties.getProperty(key);
			if(value != null && !"".equals(value.toString().trim())){
				props.put(key, value);
			}
		}
		//
		zkClient.writeData("", JsonUtils.toJson(props));
	}

	/**
	 * 
	 */
	private void initZkClient() {
		if(zkClient == null){			
			String zkServer = HttpUtils.getContent(apiBaseUrl + "/getZkServer");
			if(zkServer != null && zkServer.contains(":")){					
				ZkConnection zkConnection = new ZkConnection(zkServer);
				zkClient = new ZkClient(zkConnection, 10000);
			}
		}
	}

	private Map<String, Object> loadRemoteProperties(Properties config) throws FileNotFoundException, IOException{
		Map<String, Object> props = new HashMap<>();
		List<File> files = fecthRemoteConfigFile(config);
		for (File file : files) {
			Properties p = new Properties();
			p.load(new FileReader(file));
			
			for(String key : p.stringPropertyNames()) { 
				String value = p.getProperty(key);
				if(value != null && !"".equals(value.toString().trim())){
					props.put(key, value);
				}
			}
		}
		//
		//registerToZk(files);
		return props;
	}
	
	
	/**
	 * 注册到zk
	 * @param files
	 */
	private void registerToZk(List<File> files) {
		String nodeId;
		try {
			nodeId = InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			nodeId = UUID.randomUUID().toString();
		}
	    for (File file : files) {
	    	String path = ConfigZkPathUtils.getConfigFilePath(env, app, version, file.getName());
	    	if(!zkClient.exists(path))return;
	    	
	    	zkClient.subscribeDataChanges(path, new IZkDataListener() {
				@Override
				public void handleDataDeleted(String dataPath) throws Exception {}
				
				@Override
				public void handleDataChange(String dataPath, Object data) throws Exception {
					
				}
			});
	    	
	    	//
	    	path = path + "/nodes/" + nodeId;
	    	zkClient.createEphemeral(path);
		}
	    
	}
	
	private List<File> fecthRemoteConfigFile(Properties config) throws FileNotFoundException, IOException{
		List<File> files = new ArrayList<>(); 
		String classRootDir = Thread.currentThread().getContextClassLoader().getResource("").getPath();
		if(!classRootDir.endsWith("/"))classRootDir = classRootDir + "/";
		
		List<String> urls = parseAllRemoteUrls(config);
		for (String url : urls) {
			String fileName = url.split("file=")[1];
			logger.info("begin download remote file by url:{}",url);
			try {				
				File file = HttpUtils.downloadFile(url, classRootDir + fileName);
				if(file != null){
					files.add(file);
					logger.info("download {} ok!!",file.getPath());
				}else{
					logger.warn("download file[{}] failture from:{}",fileName,url);
				}
			} catch (Exception e) {
				logger.warn("download file[{}] failture from:{},error:{}",fileName,url,e.getMessage());
			}
		}
		
		return files;
	}
	
	private List<String> parseAllRemoteUrls(Properties config) throws FileNotFoundException, IOException{
		
		List<String> result = new ArrayList<>();
		
		//应用私有配置文件
		String fileNames = config.getProperty("app.config.files");
		if(org.apache.commons.lang3.StringUtils.isNotBlank(fileNames)){			
			String[] appFiles = StringUtils.commaDelimitedListToStringArray(fileNames);
			for (String file : appFiles) {
				result.add(String.format("%s/download?app=%s&env=%s&ver=%s&file=%s", apiBaseUrl,app,env,version,file));
			}
		}
		//全局配置文件
		fileNames = config.getProperty("global.config.files");
		if(org.apache.commons.lang3.StringUtils.isNotBlank(fileNames)){			
			String[] appFiles = StringUtils.commaDelimitedListToStringArray(fileNames);
			for (String file : appFiles) {
				result.add(String.format("%s/download?app=global&env=%s&ver=%s&file=%s", apiBaseUrl,env,version,file));
			}
		}
		
		return result;
	}


	@Override
	public void destroy() throws Exception {
		if(zkClient != null)zkClient.close();
	}
	
}
