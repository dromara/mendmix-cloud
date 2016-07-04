/**
 * 
 */
package com.jeesuite.scheduler.distribute;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.scheduler.ControlHandler;
import com.jeesuite.scheduler.SchedulerConfg;
import com.jeesuite.scheduler.SchedulerContext;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年5月3日
 */
public class ZkControlHandler implements ControlHandler,InitializingBean,DisposableBean{
	
	private static final Logger logger = LoggerFactory.getLogger(ZkControlHandler.class);
	
	private Map<String, SchedulerConfg> schedulerConfgs = new ConcurrentHashMap<>();

	private static final String ROOT = "/dis_tasks/";
	
	private String zkServers;
	
	private ZkClient zkClient;
	
	private String nodeStateParentPath;

	//是否第一个启动节点
	boolean isFirstNode = false;
	
	boolean nodeStateCreated = false;
	
	public void setZkServers(String zkServers) {
		this.zkServers = zkServers;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		ZkConnection zkConnection = new ZkConnection(zkServers);
       zkClient = new ZkClient(zkConnection, 3000);
	}

	@Override
	public synchronized void register(SchedulerConfg conf) {
		
		if(nodeStateParentPath == null){
			nodeStateParentPath = ROOT + conf.getGroupName() + "/nodes";
		}
		
		String path = getPath(conf);
		
		final String jobName = conf.getJobName();
		
		if(!zkClient.exists(nodeStateParentPath)){
			isFirstNode = true;
			zkClient.createPersistent(nodeStateParentPath, true);
		}else{
			//检查是否有节点
			if(!isFirstNode)isFirstNode = zkClient.getChildren(nodeStateParentPath).isEmpty();
		}
		
		if(!zkClient.exists(path)){
			zkClient.createPersistent(path, true);
		}
		
		//设置为默认执行节点
		conf.setCurrentNodeId(SchedulerContext.getNodeId());
		zkClient.writeData(path, JsonUtils.toJson(conf));
		schedulerConfgs.put(conf.getJobName(), conf);
		
		//创建节点临时节点
		if(!nodeStateCreated){
			zkClient.createEphemeral(nodeStateParentPath + "/" + SchedulerContext.getNodeId());
			nodeStateCreated = true;
		}
        
		//订阅同步信息变化
        zkClient.subscribeDataChanges(path, new IZkDataListener() {
			
			@Override
			public void handleDataDeleted(String dataPath) throws Exception {
				schedulerConfgs.remove(jobName);
			}
			
			@Override
			public void handleDataChange(String dataPath, Object data) throws Exception {
				if(data == null)return;
				schedulerConfgs.put(jobName, JsonUtils.toObject(data.toString(), SchedulerConfg.class));
			}
		});
        //订阅节点信息变化
        zkClient.subscribeChildChanges(nodeStateParentPath, new IZkChildListener() {
			@Override
			public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
				if(currentChilds == null || currentChilds.isEmpty())return;
				Collection<SchedulerConfg> configs = schedulerConfgs.values();
				for (SchedulerConfg config : configs) {
					if(!currentChilds.contains(config.getCurrentNodeId())){
						logger.info("process Job[{}] node_failover from {} to {}",config.getJobName(),config.getCurrentNodeId(),currentChilds.get(0));
						config.setCurrentNodeId(currentChilds.get(0));
						config.setRunning(false);
					}
				}
				
			}
		});
        
        logger.info("finish register schConfig:{}",ToStringBuilder.reflectionToString(conf, ToStringStyle.MULTI_LINE_STYLE));
	}

	private synchronized SchedulerConfg getConfigFromZK(String path,Stat stat){
		String data = stat == null ? zkClient.readData(path) : zkClient.readData(path,stat);
		return data == null ? null : JsonUtils.toObject(data, SchedulerConfg.class);
	}

	@Override
	public synchronized SchedulerConfg getConf(String jobName,boolean forceRemote) {
		SchedulerConfg config = schedulerConfgs.get(jobName);
		if(forceRemote){			
			String path = getPath(config);
			config = getConfigFromZK(path,null);
		}
		return config;
	}
	
	@Override
	public synchronized void unregister(String jobName) {
		SchedulerConfg config = schedulerConfgs.get(jobName);
		
		String path = getPath(config);
		
		if(zkClient.getChildren(nodeStateParentPath).size() == 1){
			zkClient.delete(path);
			logger.info("all node is closed ,delete path:"+path);
		}
	}
	
	private String getPath(SchedulerConfg config){
		return ROOT + config.getGroupName() + "/" + config.getJobName();
	}

	@Override
	public void destroy() throws Exception {
		zkClient.close();
	}

	@Override
	public void setRuning(String jobName, Date fireTime) {
		SchedulerConfg config = getConf(jobName,false);
		config.setRunning(true);
		config.setLastFireTime(fireTime);
		config.setCurrentNodeId(SchedulerContext.getNodeId());
		zkClient.writeData(getPath(config), JsonUtils.toJson(config));
	}

	@Override
	public void setStoping(String jobName, Date nextFireTime) {
		SchedulerConfg config = getConf(jobName,false);
		config.setRunning(false);
		config.setNextFireTime(nextFireTime);
		zkClient.writeData(getPath(config), JsonUtils.toJson(config));
	}
	
	
}
