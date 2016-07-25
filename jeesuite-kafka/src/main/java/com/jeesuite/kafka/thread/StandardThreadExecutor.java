/**
 * 
 */
package com.jeesuite.kafka.thread;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年7月20日
 */
public class StandardThreadExecutor extends ThreadPoolExecutor {
	
	private final static Logger logger = LoggerFactory.getLogger(StandardThreadExecutor.class);

	public static final int DEFAULT_MIN_THREADS = 1;
	public static final int DEFAULT_MAX_THREADS = Runtime.getRuntime().availableProcessors() + 1;
	public static final int DEFAULT_MAX_IDLE_TIME = 60 * 1000; // 1 minutes

	protected AtomicInteger submittedTasksCount;	// 正在处理的任务数 
	private int maxSubmittedTaskCount;				// 最大允许同时处理的任务数

	public StandardThreadExecutor() {
		this(DEFAULT_MIN_THREADS, DEFAULT_MAX_THREADS);
	}

	public StandardThreadExecutor(int coreThread, int maxThreads) {
		this(coreThread, maxThreads, maxThreads);
	}

	public StandardThreadExecutor(int coreThread, int maxThreads, long keepAliveTime, TimeUnit unit) {
		this(coreThread, maxThreads, keepAliveTime, unit, maxThreads);
	}

	public StandardThreadExecutor(int coreThreads, int maxThreads, int queueCapacity) {
		this(coreThreads, maxThreads, queueCapacity, Executors.defaultThreadFactory());
	}

	public StandardThreadExecutor(int coreThreads, int maxThreads, int queueCapacity, ThreadFactory threadFactory) {
		this(coreThreads, maxThreads, DEFAULT_MAX_IDLE_TIME, TimeUnit.MILLISECONDS, queueCapacity, threadFactory);
	}

	public StandardThreadExecutor(int coreThreads, int maxThreads, long keepAliveTime, TimeUnit unit, int queueCapacity) {
		this(coreThreads, maxThreads, keepAliveTime, unit, queueCapacity, Executors.defaultThreadFactory());
	}

	public StandardThreadExecutor(int coreThreads, int maxThreads, long keepAliveTime, TimeUnit unit,
			int queueCapacity, ThreadFactory threadFactory) {
		this(coreThreads, maxThreads, keepAliveTime, unit, queueCapacity, threadFactory, new AbortPolicy());
	}

	public StandardThreadExecutor(int coreThreads, int maxThreads, long keepAliveTime, TimeUnit unit,
			int queueCapacity, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(coreThreads, maxThreads, keepAliveTime, unit, new ExecutorQueue(), threadFactory, handler);
		((ExecutorQueue) getQueue()).setStandardThreadExecutor(this);

		submittedTasksCount = new AtomicInteger(0);
		
		// 最大并发任务限制： 队列buffer数 + 最大线程数 
		maxSubmittedTaskCount = queueCapacity + maxThreads; 
	}

	public void execute(Runnable command) {
		int count = submittedTasksCount.incrementAndGet();

		// 超过最大的并发任务限制，进行 reject
		// 依赖的LinkedTransferQueue没有长度限制，因此这里进行控制 
		if (count > maxSubmittedTaskCount) {
			submittedTasksCount.decrementAndGet();
			getRejectedExecutionHandler().rejectedExecution(command, this);
		}

		try {
			super.execute(command);
		} catch (RejectedExecutionException rx) {
			// there could have been contention around the queue
			if (!((ExecutorQueue) getQueue()).force(command)) {
				submittedTasksCount.decrementAndGet();

				getRejectedExecutionHandler().rejectedExecution(command, this);
			}
		}
	}

	public int getSubmittedTasksCount() {
		return this.submittedTasksCount.get();
	}
	
	public int getMaxSubmittedTaskCount() {
		return maxSubmittedTaskCount;
	}

	protected void afterExecute(Runnable r, Throwable t) {
		submittedTasksCount.decrementAndGet();
		printException(r, t);
	}
	
	/**
	 * 线程池内异常处理
	 * @param r
	 * @param t
	 */
	private void printException(Runnable r, Throwable t) {
		if (t == null && r instanceof Future<?>) {
			try {
				Future<?> future = (Future<?>) r;
				if (future.isDone())
					future.get();
			} catch (CancellationException ce) {
				t = ce;
			} catch (ExecutionException ee) {
				t = ee.getCause();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt(); // ignore/reset
			}
		}
		if (t != null)
			logger.error(t.getMessage(), t);
	}
}

class ExecutorQueue extends LinkedTransferQueue<Runnable> {
	private static final long serialVersionUID = -265236426751004839L;
	StandardThreadExecutor threadPoolExecutor;

	public ExecutorQueue() {
		super();
	}

	public void setStandardThreadExecutor(StandardThreadExecutor threadPoolExecutor) {
		this.threadPoolExecutor = threadPoolExecutor;
	}

	public boolean force(Runnable o) {
		if (threadPoolExecutor.isShutdown()) {
			throw new RejectedExecutionException("Executor not running, can't force a command into the queue");
		}
		return super.offer(o);
	}

	public boolean offer(Runnable o) {
		int poolSize = threadPoolExecutor.getPoolSize();

		if (poolSize == threadPoolExecutor.getMaximumPoolSize()) {
			return super.offer(o);
		}
		if (threadPoolExecutor.getSubmittedTasksCount() <= poolSize) {
			return super.offer(o);
		}

		if (poolSize < threadPoolExecutor.getMaximumPoolSize()) {
			return false;
		}
		return super.offer(o);
	}
}