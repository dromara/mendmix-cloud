/**
 * 
 */
package com.jeesuite.scheduler;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import com.jeesuite.common.util.DateUtils;
import com.jeesuite.scheduler.model.JobConfig;
import com.jeesuite.spring.InstanceFactory;

/**
 * 类    名：AbstractJob.java<br />
 *   
 * 功能描述：定时任务基类  <br />
 *  
 * 创建日期：2012-2-13上午11:04:13  <br />   
 * 
 * 版本信息：v 1.0<br />
 * 
 * 版权信息：Copyright (c) 2011 Csair All Rights Reserved<br />
 * 
 * 作    者：<a href="mailto:vakinge@gmail.com">vakin jiang</a><br />
 * 
 * 修改记录： <br />
 * 修 改 者    修改日期     文件版本   修改说明    
 */
public abstract class AbstractJob implements DisposableBean{
    private static final Logger logger = LoggerFactory.getLogger(AbstractJob.class);

    protected String group;
    protected String jobName;
    protected String cronExpr;
    
    protected String triggerName;
    private Scheduler scheduler;
    private CronTriggerImpl cronTrigger;
    private TriggerKey triggerKey;
    private long jobFireInterval = 0;//任务执行间隔（秒）
    
    private boolean executeOnStarted;//启动是否立即执行
    
    
    private int retries = 0;//失败重试次数
    
    private AtomicInteger runCount = new AtomicInteger(0);

	public void setGroup(String group) {
		this.group = group;
	}
	
	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getCronExpr() {
		return cronExpr;
	}

	public void setCronExpr(String cronExpr) {
		this.cronExpr = cronExpr;
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

	protected Scheduler getScheduler() {
        if (scheduler == null)
            scheduler = InstanceFactory.getInstance(Scheduler.class);
        return scheduler;
    }

	public void execute() {
		// 避免InstanceFactory还未初始化，就执行
		InstanceFactory.waitUtilInitialized();

		JobConfig schConf = JobContext.getContext().getRegistry().getConf(jobName,false);
		if (currentNodeIgnore(schConf))
			return;
		
		checkConExprChange(schConf.getCronExpr());

		Date beginTime = null;
		Exception exception = null;
		try {
			// 更新状态
			beginTime = getPreviousFireTime();
			JobContext.getContext().getRegistry().setRuning(jobName, beginTime);
			logger.debug("Job_{} at node[{}] execute begin...", jobName, JobContext.getContext().getNodeId());
			// 执行
			doJob(JobContext.getContext());
			logger.debug("Job_{} at node[{}] execute finish", jobName, JobContext.getContext().getNodeId());
		} catch (Exception e) {
			//重试
			if(retries > 0)JobContext.getContext().getRetryProcessor().submit(this, retries);
			logger.error("Job_" + jobName + " execute error", e);
			exception = e;
		}
		//执行次数累加1
		runCount.incrementAndGet();
		JobContext.getContext().getRegistry().setStoping(jobName, getTrigger().getNextFireTime(),exception);
		// 重置cronTrigger，重新获取才会更新previousFireTime，nextFireTime
		cronTrigger = null;
	}

    
    private Date getPreviousFireTime(){
    	return getTrigger().getPreviousFireTime() == null ? new Date() : getTrigger().getPreviousFireTime();
    }
    
    private Date getNextFireTime(){
    	if(getTrigger() == null)return null;
    	return getTrigger().getNextFireTime();
    }
    
    
    protected boolean currentNodeIgnore(JobConfig schConf) {
    	if(parallelEnabled())return false;
        try {
            if (!schConf.isActive()) {
            	logger.trace("Job_{} 已禁用,终止执行", jobName);
                return true;
            }
            
          //下次执行时间 < 当前时间(忽略5秒误差) 强制执行
            long currentTimes = Calendar.getInstance().getTime().getTime();
			if(schConf.getNextFireTime() != null 
            		&& currentTimes - schConf.getNextFireTime().getTime() > 5000){
            	logger.info("NextFireTime[{}] before currentTime[{}],re-join-execute task ",currentTimes,schConf.getNextFireTime().getTime());
            	return false;
            }
            
          //如果执行节点不为空,且不等于当前节点
            if(StringUtils.isNotBlank(schConf.getCurrentNodeId()) 
            		&& !JobContext.getContext().getNodeId().equals(schConf.getCurrentNodeId())){
            	logger.trace("Job_{} 当前指定执行节点:{}，不匹配当前节点:{}", jobName,schConf.getCurrentNodeId(),JobContext.getContext().getNodeId());
            	return true;
            }

            if (schConf.isRunning()) {
            	//如果某个节点开始了任务但是没有正常结束导致没有更新任务执行状态
                if (isAbnormalabort(schConf)) {
                    this.cronExpr = schConf.getCronExpr();
                    return false;
                }
                logger.debug("Job_{} 其他节点正在执行,终止当前执行", jobName);
                return true;
            }
            
            this.cronExpr = schConf.getCronExpr();
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return false;
    }
    
    private void checkConExprChange(String currentConExpr) {  
        try {   
        	if(getTrigger() == null)return;
            String originConExpression = getTrigger().getCronExpression();  
            //判断任务时间是否更新过  
            if (!originConExpression.equalsIgnoreCase(currentConExpr)) {  
            	getTrigger().setCronExpression(currentConExpr);  
                getScheduler().rescheduleJob(triggerKey, getTrigger()); 
                logger.info("Job_{} CronExpression changed, origin:{},current:{}",jobName,originConExpression,currentConExpr);
            }  
        } catch (Exception e) {
        	logger.error("checkConExprChange error",e);
        }  
         
    }  
    
    /**
     * 判断是否异常中断运行状态（）
     * @param schConf
     * @return
     */
    public boolean isAbnormalabort(JobConfig schConf){
    	if(schConf.getLastFireTime() == null)return false;
    	//上次开始执行到当前执行时长
    	long runingTime = DateUtils.getDiffSeconds(schConf.getLastFireTime(), getTrigger().getPreviousFireTime());
    	//正常阀值
    	//考虑到一些长周期任务，预定一个任务执行最长周期为1800秒
    	long threshold = getJobFireInterval() > 1800  ? 1800 : getJobFireInterval();
    	
    	if(runingTime > threshold){
    		if (logger.isDebugEnabled())
                logger.debug("Job_{} 执行时长[{}]秒,超过阀值[{}]秒，节点:{}可能发生故障,切换节点:{}", jobName,runingTime,threshold,schConf.getCurrentNodeId(),JobContext.getContext().getNodeId());
            
    		return true;
    	}
    	 
    	return false;
    }
    
    /**
     * 获取任务执行间隔
     * @return
     * @throws SchedulerException
     */
    private long getJobFireInterval(){
    	if(jobFireInterval == 0){    		
    		Date nextFireTime = getTrigger().getNextFireTime();
    		Date previousFireTime = getTrigger().getPreviousFireTime();
    		jobFireInterval = (nextFireTime.getTime() - previousFireTime.getTime())/1000;
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
		if(JobContext.getContext().getConfigPersistHandler() != null){
			JobContext.getContext().getConfigPersistHandler().merge(jobConfg);
		}
    	
        JobContext.getContext().getRegistry().register(jobConfg);
        
        logger.info("Initialized Job_{} OK!!", jobName);
    }
	
	public void afterInitialized()  {
		//启动重试任务
		if(retries > 0){
			JobContext.getContext().startRetryProcessor();
		}
		if(executeOnStarted)return;
		JobConfig conf = JobContext.getContext().getRegistry().getConf(jobName,false);
		Date nextFireTime = getNextFireTime();
		if(nextFireTime != null){			
			conf.setNextFireTime(nextFireTime);
			JobContext.getContext().getRegistry().updateJobConfig(conf);
		}
		
	}


	/**
	 * 是否开启并行处理
	 * @return
	 */
	public abstract boolean  parallelEnabled();
	
	public abstract void doJob(JobContext context) throws Exception;

}
