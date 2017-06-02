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
import java.util.List;
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

import com.jeesuite.confcenter.ConfigcenterContext;
import com.jeesuite.confcenter.utils.HttpUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月2日
 */
public class CCPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer implements DisposableBean{
	
	private final static Logger logger = LoggerFactory.getLogger(CCPropertyPlaceholderConfigurer.class);
	
	private ConfigcenterContext ccContext = ConfigcenterContext.getInstance();
	
	private ZkClient zkClient;
	
	public void setRemoteEnabled(boolean remoteEnabled) {
		ccContext.setRemoteEnabled(remoteEnabled);
	}

	@Override
	protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props)
			throws BeansException {
		super.processProperties(beanFactoryToProcess, props);
	}



	@Override
	protected Properties mergeProperties() throws IOException {
		Properties properties = super.mergeProperties();
		
		//TODO 暂时不同步zk（这一块交互逻辑还没想清楚。 同步到zk通过管理界面呈现？？ 通过zk动态更新配置？？）
		//initZkClient();
		
		URL resource = Thread.currentThread().getContextClassLoader().getResource("confcenter.properties");
		if(resource == null)throw new FileNotFoundException("配置文件[confcenter.properties]缺失");
		Properties config = new Properties();
		config.load(new FileReader(new File(resource.getPath())));
		
		ccContext.setApiBaseUrl(config.getProperty("jeesuite.configcenter.base.url"));
		ccContext.setApp(config.getProperty("jeesuite.configcenter.appName"));
		ccContext.setEnv(config.getProperty("jeesuite.configcenter.profile"));
		ccContext.setVersion(config.getProperty("jeesuite.configcenter.version","0.0.0"));

		Properties remoteProperties = ccContext.getAllRemoteProperties();
		if(remoteProperties.size() > 0){			
			properties.putAll(remoteProperties);
		}
		
		return properties;
		//
	}


	/**
	 * 
	 */
	private void initZkClient() {
		if(zkClient == null){			
			String zkServer = HttpUtils.getContent(ccContext.getApiBaseUrl() + "/getZkServer");
			if(zkServer != null && zkServer.contains(":")){					
				ZkConnection zkConnection = new ZkConnection(zkServer);
				zkClient = new ZkClient(zkConnection, 10000);
			}
		}
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
	    	String path = null;
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
	
	@Override
	public void destroy() throws Exception {
		if(zkClient != null)zkClient.close();
	}
	
}
