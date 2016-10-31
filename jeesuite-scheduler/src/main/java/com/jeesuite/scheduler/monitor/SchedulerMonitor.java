/**
 * 
 */
package com.jeesuite.scheduler.monitor;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.scheduler.model.JobConfig;
import com.jeesuite.scheduler.model.JobGroupInfo;
import com.jeesuite.scheduler.registry.ZkJobRegistry;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年10月30日
 */
public class SchedulerMonitor implements Closeable{
	

	private ZkClient zkClient;

	public SchedulerMonitor(String registryType, String servers) {
		if ("redis".equals(registryType)) {

		} else {
			ZkConnection zkConnection = new ZkConnection(servers);
			zkClient = new ZkClient(zkConnection, 3000);
		}
	}


	@Override
	public void close() throws IOException {
		if(zkClient != null)zkClient.close();
	}
	
	public List<JobGroupInfo> getAllJobGroups(){
		//zk registry
		List<JobGroupInfo> result = new ArrayList<>();
		String path = ZkJobRegistry.ROOT.substring(0,ZkJobRegistry.ROOT.length() - 1);
		List<String> groupNames = zkClient.getChildren(path);
		if(groupNames == null)return result;
		for (String groupName : groupNames) {
			JobGroupInfo groupInfo = new JobGroupInfo();
			groupInfo.setName(groupName);
			//
			path = ZkJobRegistry.ROOT + groupName;
			List<String> children = zkClient.getChildren(path);
			for (String child : children) {
				if("nodes".equals(child)){
					path = ZkJobRegistry.ROOT + groupName + "/nodes";
					groupInfo.setClusterNodes(zkClient.getChildren(path));
				}else{
					path = ZkJobRegistry.ROOT + groupName + "/" + child;
					Object data = zkClient.readData(path);
					if(data != null){						
						JobConfig jobConfig = JsonUtils.toObject(data.toString(), JobConfig.class);
						groupInfo.getJobs().add(jobConfig);
					}
				}
			}
			result.add(groupInfo);
		}
		return result;
	}
	
	public void publishEvent(MonitorCommond cmd){
		String path = ZkJobRegistry.ROOT + cmd.getJobGroup() + "/nodes";
		List<String> nodeIds = zkClient.getChildren(path);
		for (String node : nodeIds) {
			String nodePath = path + "/" + node;
			zkClient.writeData(nodePath, cmd);
		}
		
	}
	
	public static void main(String[] args) throws IOException {
		SchedulerMonitor monitor = new SchedulerMonitor("zookeeper", "127.0.0.1:2181");
		
		List<JobGroupInfo> groups = monitor.getAllJobGroups();
		System.out.println(JsonUtils.toJson(groups));
		
		monitor.close();
	}

}
