/**
 * 
 */
package com.jeesuite.scheduler;

import java.io.Closeable;
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
public class TaskRetryProcessor implements Closeable{
	
	private static final Logger logger = LoggerFactory.getLogger(TaskRetryProcessor.class);
	
	//重试时间间隔单元（毫秒）
	private static final long RETRY_PERIOD_UNIT = 15 * 1000;

	private final PriorityBlockingQueue<PriorityTask> taskQueue = new PriorityBlockingQueue<PriorityTask>(1000);  
	
	private ExecutorService executor;
	
	private AtomicBoolean closed = new AtomicBoolean(false);
	
	public TaskRetryProcessor() {
		this(1);
	}

	public TaskRetryProcessor(int poolSize) {
		executor = Executors.newFixedThreadPool(poolSize);
		executor.submit(new Runnable() {
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
		if((taskCount = taskQueue.size()) > 1000){
			logger.warn("ErrorMessageProcessor queue task count over:{}",taskCount);
		}
		taskQueue.add(new PriorityTask(job, retries));
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
			} catch (Exception e) {
				retryCount++;
				logger.warn("retry Job[{}-{}] error",job.group,job.jobName);
				retry();
			}
		}
		
		private void retry(){
			if(retryCount == retries){
				logger.warn("retry_skip mssageId[{}] retry over {} time error ,skip!!!");
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

