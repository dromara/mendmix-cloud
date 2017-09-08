package com.jeesuite.confcenter.listener;

import java.util.Map;
import java.util.Properties;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.confcenter.ConfigChangeListener;
import com.jeesuite.confcenter.ConfigcenterContext;

public class ZkConfigChangeListener implements ConfigChangeListener {
	
	private final static Logger logger = LoggerFactory.getLogger(ZkConfigChangeListener.class);

	private final static String ROOT_PATH = "/confcenter";
	private static final String NOTIFY_UPLOAD_CMD = "upload";
	private ZkClient zkClient;
	private String zkServers;

	public ZkConfigChangeListener(String zkServers) {
		this.zkServers = zkServers;
	}

	@Override
	public void register(ConfigcenterContext context) {
		ZkConnection zkConnection = new ZkConnection(zkServers);
		zkClient = new ZkClient(zkConnection, 10000);
		
		String appParentPath = ROOT_PATH + "/" + context.getEnv() + "/" + context.getApp() + "/nodes";
		if(!zkClient.exists(appParentPath)){
			zkClient.createPersistent(appParentPath, true);
		}
		
		String appNodePath = appParentPath + "/" + context.getNodeId();
		//创建node节点
		zkClient.createEphemeral(appNodePath);
		
		zkClient.subscribeDataChanges(appParentPath, new IZkDataListener() {
			@Override
			public void handleDataDeleted(String arg0) throws Exception {}
			
			@Override
			public void handleDataChange(String path, Object data) throws Exception {
				if(NOTIFY_UPLOAD_CMD.equals(data)){
					logger.info("receive cmd[{}] from path[{}]",data,path);
					if(context.pingCcServer(10)){	
						ResourceUtils.getProperty("spring.cloud.client.ipAddress");
						Properties properties = ResourceUtils.getAllProperties();
						context.syncConfigToServer(properties,false);
						logger.info("process cmd[{}] ok~",data);
					}
				}else{		
					try {						
						Map<String, Object> changeDatas = JsonUtils.toObject(data.toString(),Map.class);
						context.updateConfig(changeDatas);
					} catch (Exception e) {
						logger.error("",e);
					}
				}
				
			}
		});
	}
	

	@Override
	public void unRegister() {
		zkClient.close();
	}

	@Override
	public String typeName() {
		return "zookeepr";
	}

}
