/*
 * Copyright 2016-2022 dromara.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dromara.mendmix.scheduler;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.util.LogMessageFormat;
import org.dromara.mendmix.scheduler.model.JobConfig;
import org.dromara.mendmix.spring.InstanceFactory;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;


/**
 * 类    名：AbstractJob.java<br />
 *   
 * 功能描述：定时任务基类  <br />
 *  
 * 创建日期：2012-2-13上午11:04:13  <br />   
 * 
 * 版本信息：v 1.0<br />
 * 
 * 作    者：<a href="mailto:vakinge@gmail.com">vakin jiang</a><br />
 * 
 * 修改记录： <br />
 * 修 改 者    修改日期     文件版本   修改说明    
 */
public abstract class AbstractJob implements DisposableBean{

	private static final Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.scheduler");

  //默认允许多个节点时间误差
    private static final long DEFAULT_ALLOW_DEVIATION = 1000 * 60 * 15;
    
    private static Set<String> tenantDataSourceKeys;
    
    protected String group;
    protected String jobName;
    protected String jobAlias;
    protected String cronExpr;
    protected Date nextFireTime;
    
    protected String triggerName;
    private Scheduler scheduler;
    private CronTriggerImpl cronTrigger;
    private TriggerKey triggerKey;
    private long jobFireInterval = 0;//任务执行间隔（毫秒）
    
    
    private boolean executeOnStarted;//启动是否立即执行
    
    
    private int retries = 0;//失败重试次数
    
    private AtomicBoolean runing = new AtomicBoolean(false);

	public void setGroup(String group) {
		this.group = StringUtils.trimToNull(group);
	}
	
	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = StringUtils.trimToNull(jobName);
	}
	
	public String getJobAlias() {
		return jobAlias;
	}

	public void setJobAlias(String jobAlias) {
		this.jobAlias = jobAlias;
	}

	public String getCronExpr() {
		return cronExpr;
	}

	public void setCronExpr(String cronExpr) {
		this.cronExpr = StringUtils.trimToNull(cronExpr);
	}

	public boolean isExecuteOnStarted() {
		return executeOnStarted;
	}

	public void setExecuteOnStarted(boolean executeOnStarted) {
		this.executeOnStarted = executeOnStarted;
	}

	public String getTriggerName() {
		return triggerName;
	}

	public void setRetries(int retries) {
		this.retries = retries;
	}
	
	public static void setTenantDataSourceKeys(Set<String> tenantDataSourceKeys) {
		AbstractJob.tenantDataSourceKeys = tenantDataSourceKeys;
	}

	protected Scheduler getScheduler() {
        if (scheduler == null)
            scheduler = InstanceFactory.getInstance(Scheduler.class);
        return scheduler;
    }

	public void execute() {
		if(runing.get())return;

		ThreadLocalContext.unset();
		JobConfig schConf = getCurrentJobConfig();
		if (currentNodeIgnore(schConf)) {
			return;
		}
		runing.set(true);
		Date beginTime = null;
		Exception exception = null;
		try {
			// 更新状态
			beginTime = getPreviousFireTime();
			JobContext.getContext().getRegistry().setRuning(jobName, beginTime);
			logger.debug("<JOB-TRACE-LOGGGING> Job_{} at node[{}] execute begin...", jobName, JobContext.getContext().getNodeId());
			// 执行
			doMultiDataSourceJobs(JobContext.getContext());
			logger.debug("<JOB-TRACE-LOGGGING> Job_{} at node[{}] execute finish", jobName, JobContext.getContext().getNodeId());
		} catch (Exception e) {
			if(e instanceof MendmixBaseException == false || ((MendmixBaseException)e).getCode() != 403) {
				//重试
				if(retries > 0)JobContext.getContext().getRetryProcessor().submit(this, retries);
				logger.error("<JOB-TRACE-LOGGGING> Job_{} execute error {}", jobName, LogMessageFormat.buildExceptionMessages(e));
				exception = e;
			}
		}finally {	
			runing.set(false);
			try {
				nextFireTime = getTrigger().getNextFireTime();
				JobContext.getContext().getRegistry().setStoping(jobName, nextFireTime,exception);
				//运行日志持久化
				if(JobContext.getContext().getPersistHandler() != null){
					try {
						JobContext.getContext().getPersistHandler().saveLog(schConf, exception);
					} catch (Exception e2) {
						logger.warn("<JOB-TRACE-LOGGGING> JobLogPersistHandler run error",e2);
					}
				}
				// 重置cronTrigger，重新获取才会更新previousFireTime，nextFireTime
				cronTrigger = null;
			} catch (Exception e2) {
				e2.printStackTrace();
			}
			//
			ThreadLocalContext.unset();
		}
	}
	
    private void doMultiDataSourceJobs(JobContext context) throws Exception {
		
		if(!multiTenantDataSourceAdaptMode() || tenantDataSourceKeys.isEmpty()){
			doJob(context);
		}else{
			for (String key : tenantDataSourceKeys) {
				if(parallelEnabled() && !context.matchCurrentNode(key)) {
					logger.debug(">> doMultiDataSourceJobs_{} not matchCurrentNode for tenantDataSourceKey:{},skip",getJobName(),key);
					continue;
				}
				logger.debug(">> doMultiDataSourceJobs_{} route to tenantDataSourceKey:{}",getJobName(),key);
				//运行上下文
				CurrentRuntimeContext.setTenantDataSourceKey(key);
				try {
					doJob(context);
				} finally {
					ThreadLocalContext.unset();
				}
			}
		}
	}
	
	protected JobConfig getCurrentJobConfig() {
		JobConfig schConf;
		try {
			schConf = JobContext.getContext().getRegistry().getConf(jobName,true);
		} catch (Exception e) {
			logger.warn("getCurrentJobConfig from remote error:{}",e.getMessage());
			schConf = JobContext.getContext().getRegistry().getConf(jobName,false);
		}
		return schConf;
	}
    
	protected Date getPreviousFireTime(){
    	return getTrigger().getPreviousFireTime() == null ? new Date() : getTrigger().getPreviousFireTime();
    }
  
    
    protected boolean currentNodeIgnore(JobConfig schConf) {
    	if(parallelEnabled())return false;
        try {
            if (!schConf.isActive()) {
            	logger.debug("<JOB-TRACE-LOGGGING> Job_{} 已禁用,终止执行", jobName);
                return true;
            }
            
            //执行间隔（秒）
           // long interval = getJobFireInterval();
            long currentTimes = Calendar.getInstance().getTime().getTime();
            
            if(schConf.getNextFireTime() != null){
            	//下次执行时间 < 当前时间强制执行
            	if(currentTimes - schConf.getNextFireTime().getTime() > DEFAULT_ALLOW_DEVIATION){
                	logger.debug("<JOB-TRACE-LOGGGING> Job_{} NextFireTime[{}] before currentTime[{}],re-join-execute task ",jobName,currentTimes,schConf.getNextFireTime().getTime());
                	return false;
                }
            	//如果多个节点做了时间同步，那么误差应该为0才触发任务执行，但是考虑一些误差因素，可以做一个误差容错
//            	if(schConf.getLastFireTime() != null){            		
//            		long deviation = Math.abs(currentTimes - schConf.getLastFireTime().getTime() - interval);
//            		if(interval > 0 && deviation > DEFAULT_ALLOW_DEVIATION){
//            			logger.info("Job_{} interval:{},currentTimes:{},expect tiggertime:{}", jobName,interval,currentTimes, schConf.getLastFireTime().getTime());
//            			return true;
//            		}
//            	}
            }
			
            
          //如果执行节点不为空,且不等于当前节点
            if(StringUtils.isNotBlank(schConf.getCurrentNodeId()) ){            	
            	if(!JobContext.getContext().getNodeId().equals(schConf.getCurrentNodeId())){
            		logger.debug("<JOB-TRACE-LOGGGING> Job_{} 指定执行节点:{}，不匹配当前节点:{}", jobName,schConf.getCurrentNodeId(),JobContext.getContext().getNodeId());
            		return true;
            	}
            	//如果分配了节点，则可以保证本节点不会重复执行则不需要判断runing状态
            }else{  
            	if (schConf.isRunning()) {
            		//如果某个节点开始了任务但是没有正常结束导致没有更新任务执行状态
            		logger.info("<JOB-TRACE-LOGGGING> Job_{} 其他节点[{}]正在执行,终止当前执行", schConf.getCurrentNodeId(),jobName);
            		return true;
            	}
            }

            
            this.cronExpr = schConf.getCronExpr();
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return false;
    }
    
    public void resetTriggerCronExpr(String newCronExpr) {  
        try {   
        	if(getTrigger() == null)return;
            String originConExpression = getTrigger().getCronExpression();  
            //判断任务时间是否更新过  
            if (!originConExpression.equalsIgnoreCase(newCronExpr)) {  
            	getTrigger().setCronExpression(newCronExpr);  
                getScheduler().rescheduleJob(triggerKey, getTrigger()); 
                getScheduler().resumeTrigger(triggerKey);
                logger.info("<JOB-TRACE-LOGGGING> Job_{} CronExpression changed, origin:{},current:{}",jobName,originConExpression,newCronExpr);
            }  
        } catch (Exception e) {
        	logger.error("checkConExprChange error",e);
        }  
         
    }  
    
   
    
    /**
     * 获取任务执行间隔
     * @return
     * @throws SchedulerException
     */
    protected long getJobFireInterval(){
    	if(jobFireInterval == 0){   
    		try {				
    			Date nextFireTime = getTrigger().getNextFireTime();
    			Date previousFireTime = getTrigger().getPreviousFireTime();
    			jobFireInterval = nextFireTime.getTime() - previousFireTime.getTime();
			} catch (Exception e) {}
    	}
    	return jobFireInterval;
    }

    
    private CronTriggerImpl getTrigger() {
    	try {
    		if(this.cronTrigger == null){   
    			if(getScheduler() == null)return null;
        		Trigger trigger = getScheduler().getTrigger(triggerKey);
        		this.cronTrigger = (CronTriggerImpl)trigger;
        	}
		} catch (SchedulerException e) {
			logger.error("Job_"+jobName+" Invoke getTrigger error",e);
		}
        return cronTrigger;
    }
    
    

    @Override
	public void destroy() throws Exception {
    	JobContext.getContext().getRegistry().unregister(jobName);
    }

	public void init()  {
		
		triggerName = jobName + "Trigger";
		
		triggerKey = new TriggerKey(triggerName, group);
		
		JobConfig jobConfg = new JobConfig(group,jobName,cronExpr);
		
		//从持久化配置合并
		if(JobContext.getContext().getPersistHandler() != null){
			JobConfig persistConfig = null;
			try {persistConfig = JobContext.getContext().getPersistHandler().get(jobConfg.getJobName());} catch (Exception e) {}
			if(persistConfig != null) {
				jobConfg.setActive(persistConfig.isActive());
				jobConfg.setCronExpr(persistConfig.getCronExpr());
			}
		}
    	
        JobContext.getContext().getRegistry().register(jobConfg);
        
        logger.info("<startup-logging>  Initialized Job_{} OK!!", jobName);
    }
	
	public void afterInitialized()  {
		//启动重试任务
		if(retries > 0){
			JobContext.getContext().startRetryProcessor();
		}
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jobName == null) ? 0 : jobName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractJob other = (AbstractJob) obj;
		if (jobName == null) {
			if (other.jobName != null)
				return false;
		} else if (!jobName.equals(other.jobName))
			return false;
		return true;
	}
	
	/**
	 * 多租户数据源适配模式
	 * @return
	 */
	public boolean multiTenantDataSourceAdaptMode() {
		return true;
	}

	/**
	 * 是否开启并行处理
	 * @return
	 */
	public boolean  parallelEnabled() {
		return tenantDataSourceKeys != null && tenantDataSourceKeys.size() > 1;
	}
	
	public boolean logging() {
		return getJobFireInterval() >= 30000;
	}
	
	public boolean ignoreTenant() {
		return true;
	}
	
	public boolean forceMasterDataSource() {
		return true;
	}
	
	public abstract void doJob(JobContext context) throws Exception;

}
