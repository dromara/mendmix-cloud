/**
 * 
 */
package com.jeesuite.scheduler.registry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.scheduler.AbstractJob;
import com.jeesuite.scheduler.JobContext;
import com.jeesuite.scheduler.JobRegistry;
import com.jeesuite.scheduler.model.JobConfig;
import com.jeesuite.scheduler.monitor.MonitorCommond;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年5月3日
 */
public class ZkJobRegistry implements JobRegistry,InitializingBean,DisposableBean{
	
	private static final Logger logger = LoggerFactory.getLogger(ZkJobRegistry.class);
	
	private Map<String, JobConfig> schedulerConfgs = new ConcurrentHashMap<>();

	public static final String ROOT = "/schedulers/";
	
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
	public synchronized void register(JobConfig conf) {
		
		long currentTimeMillis = Calendar.getInstance().getTimeInMillis();
		conf.setModifyTime(currentTimeMillis);
		
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
		
		//是否要更新ZK的conf配置
		boolean updateConfInZK = isFirstNode;
		if(!updateConfInZK){
			JobConfig configFromZK = getConfigFromZK(path,null);
			updateConfInZK = configFromZK == null 
					|| !StringUtils.equals(configFromZK.getCronExpr(), conf.getCronExpr())// 执行时间修改了
					|| currentTimeMillis - configFromZK.getModifyTime() > TimeUnit.MINUTES.toMillis(10); // 
		   //拿ZK上的配置覆盖当前的
		   if(!updateConfInZK)conf = configFromZK;
		}
		
		if(updateConfInZK){
			conf.setCurrentNodeId(JobContext.getContext().getNodeId());
			zkClient.writeData(path, JsonUtils.toJson(conf)); 
		}
		schedulerConfgs.put(conf.getJobName(), conf);
		
		//创建节点临时节点
		if(!nodeStateCreated){
			zkClient.createEphemeral(nodeStateParentPath + "/" + JobContext.getContext().getNodeId());
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
				schedulerConfgs.put(jobName, JsonUtils.toObject(data.toString(), JobConfig.class));
			}
		});
        //订阅节点信息变化
        zkClient.subscribeChildChanges(nodeStateParentPath, new IZkChildListener() {
			@Override
			public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
				logger.info(">>nodes changed ,nodes:{}",currentChilds);
				if(currentChilds == null || currentChilds.isEmpty())return;
				Collection<JobConfig> configs = schedulerConfgs.values();
				for (JobConfig config : configs) {
					if(!currentChilds.contains(config.getCurrentNodeId())){
						logger.info("process Job[{}] node_failover from {} to {}",config.getJobName(),config.getCurrentNodeId(),currentChilds.get(0));
						config.setCurrentNodeId(currentChilds.get(0));
						config.setRunning(false);
					}
				}
				//
				JobContext.getContext().refreshNodes(currentChilds);
			}
		});
        
        //注册手动执行事件监听(来自于监控平台的)
        path = nodeStateParentPath + "/" + JobContext.getContext().getNodeId();
        zkClient.subscribeDataChanges(path, new IZkDataListener() {
			@Override
			public void handleDataDeleted(String dataPath) throws Exception {}
			
			@Override
			public void handleDataChange(String dataPath, Object data) throws Exception {
				MonitorCommond cmd = (MonitorCommond) data;
				execCommond(cmd);
			}
		});
        
        
        //刷新节点列表
        List<String> activeNodes = zkClient.getChildren(nodeStateParentPath);
		JobContext.getContext().refreshNodes(activeNodes);
        logger.info("finish register schConfig:{}，\nactiveNodes:{}",ToStringBuilder.reflectionToString(conf, ToStringStyle.MULTI_LINE_STYLE),activeNodes);
	}

	private synchronized JobConfig getConfigFromZK(String path,Stat stat){
		Object data = stat == null ? zkClient.readData(path) : zkClient.readData(path,stat);
		return data == null ? null : JsonUtils.toObject(data.toString(), JobConfig.class);
	}

	@Override
	public synchronized JobConfig getConf(String jobName,boolean forceRemote) {
		JobConfig config = schedulerConfgs.get(jobName);
		if(forceRemote){			
			String path = getPath(config);
			config = getConfigFromZK(path,null);
		}
		return config;
	}
	
	@Override
	public synchronized void unregister(String jobName) {
		JobConfig config = schedulerConfgs.get(jobName);
		
		String path = getPath(config);
		
		if(zkClient.getChildren(nodeStateParentPath).size() == 1){
			zkClient.delete(path);
			logger.info("all node is closed ,delete path:"+path);
		}
	}
	
	private String getPath(JobConfig config){
		return ROOT + config.getGroupName() + "/" + config.getJobName();
	}

	@Override
	public void destroy() throws Exception {
		zkClient.close();
	}

	@Override
	public void setRuning(String jobName, Date fireTime) {
		JobConfig config = getConf(jobName,false);
		config.setRunning(true);
		config.setLastFireTime(fireTime);
		config.setCurrentNodeId(JobContext.getContext().getNodeId());
		config.setModifyTime(Calendar.getInstance().getTimeInMillis());
		zkClient.writeData(getPath(config), JsonUtils.toJson(config));
	}

	@Override
	public void setStoping(String jobName, Date nextFireTime) {
		JobConfig config = getConf(jobName,false);
		config.setRunning(false);
		config.setNextFireTime(nextFireTime);
		config.setModifyTime(Calendar.getInstance().getTimeInMillis());
		zkClient.writeData(getPath(config), JsonUtils.toJson(config));
	}


	@Override
	public List<JobConfig> getAllJobs() {
		return new ArrayList<>(schedulerConfgs.values());
	}
	
	private void execCommond(MonitorCommond cmd){
		if(cmd == null)return;
		switch (cmd.getCmdType()) {
		case MonitorCommond.TYPE_EXEC:
			String key = cmd.getJobGroup() + ":" + cmd.getJobName();
			final AbstractJob abstractJob = JobContext.getContext().getAllJobs().get(key);
			if(abstractJob != null){
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							abstractJob.doJob(JobContext.getContext());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).start();
			}
			break;
		case MonitorCommond.TYPE_STATUS_MOD:
			//TODO 
		default:
			break;
		}
	}
}
