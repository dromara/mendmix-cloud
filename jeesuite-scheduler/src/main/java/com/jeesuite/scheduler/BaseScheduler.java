/**
 * 
 */
package com.jeesuite.scheduler;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import com.jeesuite.common.util.DateUtils;
import com.jeesuite.spring.InstanceFactory;

/**
 * 类    名：BaseScheduler.java<br />
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
public abstract class BaseScheduler implements DisposableBean{
    private static final Logger logger = LoggerFactory.getLogger(BaseScheduler.class);

    protected String group;
    protected String jobName;
    protected String cronExpr;
    
    protected String triggerName;
    private Scheduler scheduler;
    private CronTriggerImpl cronTrigger;
    private TriggerKey triggerKey;
    private long jobFireInterval = 0;//任务执行间隔（秒）
    
    private boolean promptlyExecute;//启动是否立即执行
    
	//@Autowired
    private ControlHandler controlHandler;

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

	public void setControlHandler(ControlHandler controlHandler) {
		this.controlHandler = controlHandler;
	}
	
	public boolean isPromptlyExecute() {
		return promptlyExecute;
	}

	public void setPromptlyExecute(boolean promptlyExecute) {
		this.promptlyExecute = promptlyExecute;
	}

	protected Scheduler getScheduler() {
        if (scheduler == null)
            scheduler = InstanceFactory.getInstance(Scheduler.class);
        return scheduler;
    }

    public void execute() {
    	//避免InstanceFactory还未初始化，就执行
    	if(scheduler == null && InstanceFactory.getInstanceProvider() == null){
    		while(true){
    			if(InstanceFactory.getInstanceProvider() != null)break;
    			try {Thread.sleep(500);} catch (Exception e) {}
    		}
    	}
    	
        String latestConExpr = null;
        Date beginTime = null;
        boolean runing = false;
        try {
            latestConExpr = checkExecutable();
            if (latestConExpr == null)
                return;
            // 更新状态
            beginTime = getPreviousFireTime();
			controlHandler.setRuning(jobName, beginTime);
            runing = true;
            logger.debug("Job_{} at node[{}] execute begin...",jobName,SchedulerContext.getNodeId());
            //执行
            doJob();
            logger.debug("Job_{} at node[{}] execute finish",jobName,SchedulerContext.getNodeId());
            //检查是否更新了执行策略
            checkConExpr(latestConExpr);
        } catch (Exception e) {
            logger.error("Job_" + jobName + " execute error", e);
        } finally {
            if (runing) {
            	controlHandler.setStoping(jobName,getTrigger().getNextFireTime());
            }
          //重置cronTrigger，重新获取才会更新previousFireTime，nextFireTime
        	cronTrigger = null;
        }
    }

    public void checkConExpr(String latestConExpr) {
        try {
            //
            String originConExpression = getTrigger().getCronExpression();
            //判断任务时间是否更新过
            if (!originConExpression.equalsIgnoreCase(latestConExpr)) {
            	logger.info("reset ConExpression[{}] To [{}] ",originConExpression,latestConExpr);
            	getTrigger().setCronExpression(latestConExpr);
                getScheduler().rescheduleJob(triggerKey, getTrigger());
                //重置
                jobFireInterval = 0;
            }
        } catch (Exception e) {
            // TODO: handle exception
        	e.printStackTrace();
        }
    }

    /**
     * 检测当前是否可用(确保当前及其集群环境仅有一个线程在运行该任务)<br>
     * generate by: vakin jiang
     *                    at 2012-2-13
     * @return
     */
    protected String checkExecutable() {
        try {
            SchedulerConfg schConf = controlHandler.getConf(jobName,false);
            if (!schConf.isActive()) {
                if (logger.isDebugEnabled())
                    logger.debug("Job_{} 已禁用,终止执行", jobName);
                return null;
            }
            
            //
//            if(schConf.getNextFireTime() != null && getTrigger().getPreviousFireTime().compareTo(schConf.getNextFireTime()) < 0 && schConf.getNextFireTime().after(new Date())){
//            	if (logger.isDebugEnabled())
//                    logger.debug("Job_{} 下次执行时间:{},当前时间:{}不满足", jobName,DateUtils.format(schConf.getNextFireTime()),DateUtils.format(getTrigger().getPreviousFireTime()));
//            	return null;
//            }
            
            //
            if (schConf.isRunning()) {
            	//如果某个节点开始了任务但是没有正常结束导致没有更新任务执行状态
                if (isAbnormalabort(schConf)) {
                    this.cronExpr = schConf.getCronExpr();
                    return this.cronExpr;
                }
                if (logger.isDebugEnabled())
                    logger.debug(triggerName + "Job_{} 其他节点正在执行,终止当前执行", jobName);
                return null;
            }
            //如果执行节点不为空,且不等于当前节点
            if(StringUtils.isNotBlank(schConf.getCurrentNodeId()) 
            		&& !SchedulerContext.getNodeId().equals(schConf.getCurrentNodeId())){
            	if(isAbnormalabort(schConf)){
            		this.cronExpr = schConf.getCronExpr();
            		return this.cronExpr;
            	}
            	return null;
            }
            this.cronExpr = schConf.getCronExpr();
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return this.cronExpr;
    }
    
    private Date getPreviousFireTime(){
    	return getTrigger().getPreviousFireTime() == null ? new Date() : getTrigger().getPreviousFireTime();
    }
    
    private Date getNextFireTime(){
    	return getTrigger().getNextFireTime();
    }
    
    
    /**
     * 判断是否异常中断运行状态（）
     * @param schConf
     * @return
     */
    public boolean isAbnormalabort(SchedulerConfg schConf){
    	if(schConf.getLastFireTime() == null)return false;
    	//上次开始执行到当前执行时长
    	long runingTime = DateUtils.getDiffSeconds(schConf.getLastFireTime(), getTrigger().getPreviousFireTime());
    	//正常阀值
    	//考虑到一些长周期任务，预定一个任务执行最长周期为600秒
    	long threshold = getJobFireInterval() > 600  ? 600 : getJobFireInterval();
    	
    	if(runingTime > threshold){
    		if (logger.isDebugEnabled())
                logger.debug("Job_{} 执行时长[{}]秒,超过阀值[{}]秒，节点:{}可能发生故障,切换节点:{}", jobName,runingTime,threshold,schConf.getCurrentNodeId(),SchedulerContext.getNodeId());
            
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
    	controlHandler.unregister(jobName);
    }

	public void init()  {
		
		Validate.notNull(controlHandler);
		
		triggerName = jobName + "Trigger";
		
		triggerKey = new TriggerKey(triggerName, group);
		
		SchedulerConfg schedulerConfg = new SchedulerConfg(group,jobName,cronExpr);
    	
        controlHandler.register(schedulerConfg);
        
        logger.info("Initialized Job_{} OK!!", jobName);
    }


    public abstract void doJob() throws Exception;

}
