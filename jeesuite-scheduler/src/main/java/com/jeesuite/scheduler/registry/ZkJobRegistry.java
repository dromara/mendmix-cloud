/**
 * 
 */
package com.jeesuite.scheduler.registry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.zookeeper.data.Stat;
import org.quartz.CronExpression;
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
	
	private String groupPath;
	
	private String nodeStateParentPath;
	
	private ScheduledExecutorService zkCheckTask;
	
	private volatile boolean zkAvailabled = true;
	
	private volatile boolean updatingStatus;
	
	private boolean nodeEventSubscribed = false;
	
	public void setZkServers(String zkServers) {
		this.zkServers = zkServers;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		ZkConnection zkConnection = new ZkConnection(zkServers);
       zkClient = new ZkClient(zkConnection, 10000);
       //
       zkCheckTask = Executors.newScheduledThreadPool(1);
       
		zkCheckTask.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if (schedulerConfgs.isEmpty())
					return;
				List<String> activeNodes = null;
				try {
					activeNodes = zkClient.getChildren(nodeStateParentPath);
					zkAvailabled = true;
				} catch (Exception e) {
					checkZkAvailabled();
					activeNodes = new ArrayList<>(JobContext.getContext().getActiveNodes());
				}
				
				if(!activeNodes.contains(JobContext.getContext().getNodeId())){
					zkClient.createEphemeral(nodeStateParentPath + "/" + JobContext.getContext().getNodeId());
					logger.info("node[{}] re-join task clusters",JobContext.getContext().getNodeId());
				}
				//对节点列表排序
				Collections.sort(activeNodes);
				//本地缓存的所有jobs
				Collection<JobConfig> jobConfigs = schedulerConfgs.values();
				//
				for (JobConfig jobConfig : jobConfigs) {						
					// 如果本地任务指定的执行节点不在当前实际的节点列表，重新指定
					if (!activeNodes.contains(jobConfig.getCurrentNodeId())) {
						//指定当前节点为排序后的第一个节点
						String newExecuteNodeId = activeNodes.get(0);
						jobConfig.setCurrentNodeId(newExecuteNodeId);
						logger.warn("Job[{}-{}] currentNodeId[{}] not in activeNodeList, assign new ExecuteNodeId:{}",jobConfig.getGroupName(),jobConfig.getJobName(),jobConfig.getCurrentNodeId(),newExecuteNodeId);
					}
				}
			}
		}, 60, 30, TimeUnit.SECONDS);
	}

	@Override
	public synchronized void register(JobConfig conf) {	
		//是否第一个启动节点
		boolean isFirstNode = false;
		
		Calendar now = Calendar.getInstance();
		long currentTimeMillis = now.getTimeInMillis();
		conf.setModifyTime(currentTimeMillis);
		
		if(groupPath == null){
			groupPath = ROOT + conf.getGroupName();
		}
		if(nodeStateParentPath == null){
			nodeStateParentPath = groupPath + "/nodes";
		}
		
		String path = getPath(conf);
		
		final String jobName = conf.getJobName();
		
		if(!zkClient.exists(nodeStateParentPath)){
			isFirstNode = true;
			zkClient.createPersistent(nodeStateParentPath, true);
		}else{
			//检查是否有节点
			if(!isFirstNode){
				isFirstNode = zkClient.getChildren(nodeStateParentPath).size() == 0;
			}
		}
		
		if(!zkClient.exists(path)){
			zkClient.createPersistent(path, true);
		}
		
		//是否要更新ZK的conf配置
		boolean updateConfInZK = isFirstNode;
		if(!updateConfInZK){
			JobConfig configFromZK = getConfigFromZK(path,null);
			if(configFromZK != null){
				//1.当前执行时间策略变化了
				//2.下一次执行时间在当前时间之前
				//3.配置文件修改是30分钟前
				if(!StringUtils.equals(configFromZK.getCronExpr(), conf.getCronExpr())){
					updateConfInZK = true;
				}else if(configFromZK.getNextFireTime() != null && configFromZK.getNextFireTime().before(now.getTime())){
					updateConfInZK = true;
				}else if(currentTimeMillis - configFromZK.getModifyTime() > TimeUnit.MINUTES.toMillis(30)){
					updateConfInZK = true;
				}
			}else{
				//zookeeper 该job不存在？
				updateConfInZK = true;
			}
		   //拿ZK上的配置覆盖当前的
		   if(!updateConfInZK){
			   conf = configFromZK;
		   }
		}
		
		if(updateConfInZK){
			conf.setCurrentNodeId(JobContext.getContext().getNodeId());
			zkClient.writeData(path, JsonUtils.toJson(conf)); 
		}
		schedulerConfgs.put(conf.getJobName(), conf);
		
		//
		regAndSubscribeNodeEvent();
        
		//订阅同步信息变化
        zkClient.subscribeDataChanges(path, new IZkDataListener() {
			
			@Override
			public void handleDataDeleted(String dataPath) throws Exception {
				schedulerConfgs.remove(jobName);
			}
			
			@Override
			public void handleDataChange(String dataPath, Object data) throws Exception {
				if(data == null)return;
				JobConfig _jobConfig = JsonUtils.toObject(data.toString(), JobConfig.class);
				schedulerConfgs.put(jobName, _jobConfig);
			}
		});
        
        //清除之前遗留节点
        if(isFirstNode){
        	List<String> historyJobNames = zkClient.getChildren(groupPath);
        	for (String name : historyJobNames) {
				if("nodes".equals(name) || conf.getJobName().equals(name))continue;
				zkClient.delete(groupPath + "/" + name);
				logger.info("delete history job path:{}/{}",groupPath,name);
			}
        	
        }
        
        logger.info("finish register schConfig:{}",ToStringBuilder.reflectionToString(conf, ToStringStyle.MULTI_LINE_STYLE));
	}

	/**
	 * 订阅节点事件
	 * @return
	 */
	private synchronized void regAndSubscribeNodeEvent() {
		if(nodeEventSubscribed)return;
		//创建node节点
		zkClient.createEphemeral(nodeStateParentPath + "/" + JobContext.getContext().getNodeId());
		
		String path;
		//订阅节点信息变化
        zkClient.subscribeChildChanges(nodeStateParentPath, new IZkChildListener() {
			@Override
			public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
				//
				if(currentChilds == null || !currentChilds.contains(JobContext.getContext().getNodeId())){
					zkClient.createEphemeral(nodeStateParentPath + "/" + JobContext.getContext().getNodeId());
					logger.info("Nodelist is empty~ node[{}] re-join task clusters",JobContext.getContext().getNodeId());
					return;
				}
				logger.info(">>nodes changed ,nodes:{}",currentChilds);
				//分配节点
				rebalanceJobNode(currentChilds);
				//刷新当前可用节点
				JobContext.getContext().refreshNodes(currentChilds);
			}
		});
        
        logger.info("subscribe nodes change event at path:{}",nodeStateParentPath);
        
        //注册手动执行事件监听(来自于监控平台的)
        path = nodeStateParentPath + "/" + JobContext.getContext().getNodeId();
        zkClient.subscribeDataChanges(path, new IZkDataListener() {
			@Override
			public void handleDataDeleted(String dataPath) throws Exception {}
			@Override
			public void handleDataChange(String dataPath, Object data) throws Exception {
				MonitorCommond cmd = (MonitorCommond) data;
				logger.info("收到commond:" + cmd.toString());
				execCommond(cmd);
			}
		});
        
        logger.info("subscribe command event at path:{}",path);
        
        
        //刷新节点列表
        List<String> activeNodes = zkClient.getChildren(nodeStateParentPath);
		JobContext.getContext().refreshNodes(activeNodes);
		
		logger.info("current activeNodes:{}",activeNodes);
		nodeEventSubscribed = true;
	}
	
	/**
	 * 重新分配执行节点
	 * @param nodes
	 */
	private synchronized void rebalanceJobNode(List<String> nodes) {
		while(updatingStatus);
		Collection<JobConfig> jobs = schedulerConfgs.values();
		int nodeIndex = 0;
		for (JobConfig job : jobs) {
			String nodeId = nodes.get(nodeIndex++);
			if(!StringUtils.equals(job.getCurrentNodeId(), nodeId)){				
				job.setCurrentNodeId(nodeId);
				logger.info("rebalance Job[{}-{}] To Node[{}] ",job.getGroupName(),job.getJobName(),nodeId);
			}
			if(nodeIndex >= nodes.size()){
				nodeIndex = 0;
			}
			//
			updateJobConfig(job);
		}
		
	}

	private synchronized JobConfig getConfigFromZK(String path,Stat stat){
		Object data = stat == null ? zkClient.readData(path) : zkClient.readData(path,stat);
		return data == null ? null : JsonUtils.toObject(data.toString(), JobConfig.class);
	}

	@Override
	public synchronized JobConfig getConf(String jobName,boolean forceRemote) {
		JobConfig config = schedulerConfgs.get(jobName);
		//如果只有一个节点就不从强制同步了
		if(schedulerConfgs.size() == 1){
			config.setCurrentNodeId(JobContext.getContext().getNodeId());
			return config;
		}
		if(forceRemote){			
			String path = getPath(config);
			try {				
				config = getConfigFromZK(path,null);
			} catch (Exception e) {
				checkZkAvailabled();
				logger.warn("fecth JobConfig from Registry error",e);
			}
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
		zkCheckTask.shutdown();
		zkClient.close();
	}

	@Override
	public void setRuning(String jobName, Date fireTime) {
		updatingStatus = false;
		try {			
			JobConfig config = getConf(jobName,false);
			config.setRunning(true);
			config.setLastFireTime(fireTime);
			config.setCurrentNodeId(JobContext.getContext().getNodeId());
			config.setModifyTime(Calendar.getInstance().getTimeInMillis());
			config.setErrorMsg(null);
			//更新本地
			schedulerConfgs.put(jobName, config);
			try {			
				if(zkAvailabled)zkClient.writeData(getPath(config), JsonUtils.toJson(config));
			} catch (Exception e) {
				checkZkAvailabled();
				logger.warn(String.format("Job[{}] setRuning error...", jobName),e);
			}
		} finally {
			updatingStatus = false;
		}
	}

	@Override
	public void setStoping(String jobName, Date nextFireTime,Exception e) {
		updatingStatus = false;
		try {
			JobConfig config = getConf(jobName,false);
			config.setRunning(false);
			config.setNextFireTime(nextFireTime);
			config.setModifyTime(Calendar.getInstance().getTimeInMillis());
			config.setErrorMsg(e == null ? null : e.getMessage());
			//更新本地
			schedulerConfgs.put(jobName, config);
			try {		
				if(zkAvailabled)zkClient.writeData(getPath(config), JsonUtils.toJson(config));
			} catch (Exception ex) {
				checkZkAvailabled();
				logger.warn(String.format("Job[{}] setStoping error...", jobName),ex);
			}
		} finally {
			updatingStatus = false;
		}
		
	}


	@Override
	public List<JobConfig> getAllJobs() {
		return new ArrayList<>(schedulerConfgs.values());
	}
	
	private boolean checkZkAvailabled(){
		try {
			zkClient.exists(ROOT);
			zkAvailabled = true;
		} catch (Exception e) {
			zkAvailabled = false;
			logger.warn("ZK server is not available....");
		}
		return zkAvailabled;
	}
	
	private void execCommond(MonitorCommond cmd){
		if(cmd == null)return;
		
		JobConfig config = schedulerConfgs.get(cmd.getJobName());
		String key = cmd.getJobGroup() + ":" + cmd.getJobName();
		final AbstractJob abstractJob = JobContext.getContext().getAllJobs().get(key);
		if(MonitorCommond.TYPE_EXEC == cmd.getCmdType()){
			if(config.isRunning()){
				logger.info("任务正在执行中，请稍后再执行");
				return;
			}
			if(abstractJob != null){
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							logger.info("begin execute job[{}] by MonitorCommond",abstractJob.getJobName());
							abstractJob.doJob(JobContext.getContext());
						} catch (Exception e) {
							logger.error(abstractJob.getJobName(),e);
						}
					}
				}).start();
			}else{
				logger.warn("Not found job by key:{} !!!!",key);
			}
		}else if(MonitorCommond.TYPE_STATUS_MOD == cmd.getCmdType() 
				|| MonitorCommond.TYPE_CRON_MOD == cmd.getCmdType()){
			
			if(config != null){
				if(MonitorCommond.TYPE_STATUS_MOD == cmd.getCmdType()){					
					config.setActive("1".equals(cmd.getBody()));
				}else{
					try {
						new CronExpression(cmd.getBody().toString());
					} catch (Exception e) {
						throw new RuntimeException("cron表达式格式错误");
					}
					abstractJob.resetTriggerCronExpr(cmd.getBody().toString());
					config.setCronExpr(cmd.getBody().toString());
					
				}
				updateJobConfig(config);
				if(JobContext.getContext().getConfigPersistHandler() != null){
					JobContext.getContext().getConfigPersistHandler().persist(config);
				}
			}
		}
	}

	@Override
	public void updateJobConfig(JobConfig config) {
		config.setModifyTime(Calendar.getInstance().getTimeInMillis());
		zkClient.writeData(getPath(config), JsonUtils.toJson(config));
		schedulerConfgs.put(config.getJobName(), config);
	}
	
}
