/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.scheduler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月16日
 */
public class TaskRetryProcessor {
	
	private static final Logger logger = LoggerFactory.getLogger(TaskRetryProcessor.class);
	
	//重试时间间隔单元（毫秒）
	private static final long RETRY_PERIOD_UNIT = 10 * 1000;

	private final PriorityBlockingQueue<PriorityTask> taskQueue = new PriorityBlockingQueue<PriorityTask>(1000);  
	
	private ExecutorService executor;
	
	private AtomicBoolean closed = new AtomicBoolean(false);
	
	private List<String> queueJobNames = new CopyOnWriteArrayList<>();
	
	public TaskRetryProcessor() {
		this(1);
	}

	public TaskRetryProcessor(int poolSize) {
		executor = Executors.newFixedThreadPool(poolSize);
		executor.execute(new Runnable() {
			@Override
			public void run() {
				while(!closed.get()){
					try {
						PriorityTask task = taskQueue.take();
						//空任务跳出循环
						if(task.getJob() == null)break;
						if(task.nextFireTime - System.currentTimeMillis() > 0){
							TimeUnit.MILLISECONDS.sleep(1000);
							taskQueue.put(task);
							continue;
						}
						task.run();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	public void submit(final AbstractJob job,final int retries){
		int taskCount;
		if((taskCount = taskQueue.size()) > 100){
			logger.warn("ErrorMessageProcessor queue task count over:{}",taskCount);
		}
		if(queueJobNames.contains(job.jobName)){
			logger.debug("Job[{}-{}] is existing in retry Queue",job.group,job.jobName);
			return;
		}
		logger.info("Add Job[{}-{}] to retry Queue,will be retry {} time",job.group,job.jobName,retries);
		taskQueue.add(new PriorityTask(job, retries));
		queueJobNames.add(job.jobName);
	}
	
	public void close(){
		closed.set(true);
		//taskQueue里面没有任务会一直阻塞，所以先add一个新任务保证执行
		taskQueue.add(new PriorityTask(null, 0));
		try {Thread.sleep(1000);} catch (Exception e) {}
		executor.shutdown();
		logger.info("TaskRetryProcessor closed");
	}
	
	class PriorityTask implements Runnable,Comparable<PriorityTask>{

		final AbstractJob job;
		
		int retries;
		int retryCount = 0;
	    long nextFireTime;
		
	    public PriorityTask(AbstractJob job, int retries) {
	    	this(job, retries, System.currentTimeMillis() + RETRY_PERIOD_UNIT);
	    }
	    
		public PriorityTask(AbstractJob job, int retries,long nextFireTime) {
			super();
			this.retries = retries;
			this.job = job;
			this.nextFireTime = nextFireTime;
		}

		public AbstractJob getJob() {
			return job;
		}

		@Override
		public void run() {
			try {	
				logger.debug("begin re-process Job[{}-{}]:",job.group,job.jobName);
				job.doJob(JobContext.getContext());
				//remove
				queueJobNames.remove(job.jobName);
			} catch (Exception e) {
				retryCount++;
				logger.warn("retry Job[{}-{}] error",job.group,job.jobName);
				retry();
			}
		}
		
		private void retry(){
			if(retryCount == retries){
				logger.warn("retry_skip mssageId[{}] retry over {} time error ,skip!!!");
				//remove
				queueJobNames.remove(job.jobName);
				return;
			}
			nextFireTime = nextFireTime + retryCount * RETRY_PERIOD_UNIT;
			//重新放入任务队列
			taskQueue.add(this);
			logger.debug("re-submit Job[{}-{}] task to queue,next fireTime:{}",job.group,job.jobName,nextFireTime);
		}

		@Override
		public int compareTo(PriorityTask o) {
			return (int) (this.nextFireTime - o.nextFireTime);
		}

		
		
	}

}

