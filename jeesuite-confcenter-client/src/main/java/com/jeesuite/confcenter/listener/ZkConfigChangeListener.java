package com.jeesuite.confcenter.listener;

import java.util.Map;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.confcenter.ConfigChangeListener;
import com.jeesuite.confcenter.ConfigcenterContext;

public class ZkConfigChangeListener implements ConfigChangeListener {

	private final static String ROOT_PATH = "/confcenter";
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
			public void handleDataChange(String arg0, Object arg1) throws Exception {
				Map<String, Object> changeDatas = JsonUtils.toObject(arg1.toString(),Map.class);
				context.updateConfig(changeDatas);
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
