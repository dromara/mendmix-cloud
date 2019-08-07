/**
 * 
 */
package com.jeesuite.scheduler.monitor;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.scheduler.JobContext;
import com.jeesuite.scheduler.model.JobConfig;
import com.jeesuite.scheduler.model.JobGroupInfo;
import com.jeesuite.scheduler.registry.ZkJobRegistry;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年10月30日
 */
public class SchedulerMonitor implements Closeable{
	
	private static final Logger logger = LoggerFactory.getLogger(SchedulerMonitor.class);

	private AsyncEventBus asyncEventBus;
	private ZkClient zkClient;

	public SchedulerMonitor(String registryType, String servers) {
		if("zookeeper".equals(registryType)) {
			ZkConnection zkConnection = new ZkConnection(servers);
			zkClient = new ZkClient(zkConnection, 3000);
		}else{
			asyncEventBus = new AsyncEventBus(JobContext.getContext().getSyncExecutor());
			asyncEventBus.register(JobContext.getContext().getRegistry());
		}
	}

	/**
	 * @param zkClient
	 */
	public SchedulerMonitor(ZkClient zkClient) {
		this.zkClient = zkClient;
	}

	@Override
	public void close() throws IOException {
		if(zkClient != null)zkClient.close();
	}
	
	public JobGroupInfo getJobGroupInfo(String groupName){

		if(StringUtils.isBlank(groupName)){
			logger.warn("getJobGroupInfo groupName is required");
			return null;
		}
		JobGroupInfo groupInfo = new JobGroupInfo();
		groupInfo.setName(groupName);
        if(asyncEventBus != null){
        	groupInfo.setJobs(JobContext.getContext().getRegistry().getAllJobs());
        	return groupInfo;
        }
		//
		String path = ZkJobRegistry.ROOT + groupName;
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
		
		if(groupInfo.getClusterNodes().size() > 0){				
			return groupInfo;
		}
		
		return null;
	
	}
	
	public List<String> getGroups(){
		if(asyncEventBus != null){
			List<String> groups = new ArrayList<>();
			Set<String> jobKeys = JobContext.getContext().getAllJobs().keySet();
			for (String jobKey : jobKeys) {
				groups.add(StringUtils.splitByWholeSeparator(jobKey, ":")[0]);
			}
			return groups;
		}
		String path = ZkJobRegistry.ROOT.substring(0,ZkJobRegistry.ROOT.length() - 1);
		return zkClient.getChildren(path);
	}
	
	public List<JobGroupInfo> getAllJobGroups(){
		//zk registry
		List<JobGroupInfo> result = new ArrayList<>();
		List<String> groupNames = getGroups();
		if(groupNames == null)return result;
		for (String groupName : groupNames) {
			JobGroupInfo groupInfo = getJobGroupInfo(groupName);
			
			if(groupInfo != null){				
				result.add(groupInfo);
			}
		}
		return result;
	}
	
	public void publishEvent(MonitorCommond cmd){
		if(asyncEventBus != null){
			asyncEventBus.post(cmd);
			return;
		}
		String path = ZkJobRegistry.ROOT + cmd.getJobGroup() + "/nodes";
		List<String> nodeIds = zkClient.getChildren(path);
		for (String node : nodeIds) {
			String nodePath = path + "/" + node;
			zkClient.writeData(nodePath, cmd);
			logger.info("publishEvent finish，path:{},content:{}",nodePath,cmd);
			break;
		}
		
	}
	
	public void clearInvalidGroup(){
        if(asyncEventBus != null)return;
    	List<String> groups = zkClient.getChildren(ZkJobRegistry.ROOT.substring(0, ZkJobRegistry.ROOT.length() - 1));
    	logger.info("==============clear Invalid jobs=================");
    	for (String group : groups) {
    		String groupPath = ZkJobRegistry.ROOT + group;
    		String nodeStateParentPath = groupPath + "/nodes";
    		try {
    			if(zkClient.exists(nodeStateParentPath) == false || zkClient.countChildren(nodeStateParentPath) == 0){
    				List<String> jobs = zkClient.getChildren(groupPath);
    				for (String job : jobs) {
    					zkClient.delete(groupPath + "/" + job);
    					logger.info("delete path:{}/{}",groupPath,job);
    				}
    				zkClient.delete(groupPath);
    				logger.info("delete path:{}",groupPath);
    			}
			} catch (Exception e) {}
		}
    	logger.info("==============clear Invalid jobs end=================");
    	
    
	}
	
	public static void main(String[] args) throws IOException {
		SchedulerMonitor monitor = new SchedulerMonitor("zookeeper", "127.0.0.1:2181");
		
		List<JobGroupInfo> groups = monitor.getAllJobGroups();
		System.out.println(JsonUtils.toJson(groups));
		
		monitor.close();
	}

}
