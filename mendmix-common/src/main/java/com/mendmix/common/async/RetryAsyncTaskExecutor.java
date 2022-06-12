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
package com.mendmix.common.async;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.async.StandardThreadExecutor.StandardThreadFactory;

/**
 * 异步任务执行器
 * <br>
 * Class Name   : RetryAsyncTaskExecutor
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年2月18日
 */
public class RetryAsyncTaskExecutor {

	private static Logger logger = LoggerFactory.getLogger("com.mendmix.core.async");
	
	private static ThreadLocal<String> traceIdHolder = new ThreadLocal<>();
	
	private StandardThreadExecutor executor;
	private int maxRetry;
	
	private static volatile RetryAsyncTaskExecutor defaultExecutor;
	
	private static RetryAsyncTaskExecutor getDefault() {
		if(defaultExecutor != null)return defaultExecutor;
		synchronized (RetryAsyncTaskExecutor.class) {
			if(defaultExecutor != null)return defaultExecutor;
			defaultExecutor = new RetryAsyncTaskExecutor("__default", 10, 2000, 1);
		}
		return defaultExecutor;
	}
	
	/**
	 * 
	 * @param taskName 执行器名
	 * @param threads 执行最大线程数（空闲会回收）
	 * @param queueSize 任务队列
	 * @param maxRetry 最大重试次数
	 */
	public RetryAsyncTaskExecutor(String taskName, int threads, int queueSize,int maxRetry) {
		this.maxRetry = maxRetry;
		executor = new StandardThreadExecutor(1, threads, 300, TimeUnit.SECONDS, queueSize, new StandardThreadFactory(taskName + "-asyncTask"));
		logger.info("MENDMIX-TRACE-LOGGGING-->> AsyncTaskExecutor[{}] 初始化完成 -> threads:{},queueSize:{},maxRetry:{}",taskName,threads,queueSize,maxRetry);
	}

	public void submitTask(RetryTask task){
		executeWithRetry(task,0);
	}
	
	public static void execute(RetryTask task){
		getDefault().executeWithRetry(task,0);
	}
	
	private void executeWithRetry(RetryTask task,int execNums) {
		final String tenantId = CurrentRuntimeContext.getTenantId();
		if(execNums >= maxRetry){	
			logger.warn("MENDMIX-TRACE-LOGGGING-->> {} executeWithRetry over maxRetry[{}]",task.traceId(),maxRetry);
			onFinalErrorProcess(task);
			return;
		}

		final int currentExecNums =  (++execNums);
		if(currentExecNums > 1){
			try {Thread.sleep(RandomUtils.nextLong(500, 1000));} catch (Exception e) {}
			logger.info("MENDMIX-TRACE-LOGGGING-->> {} {}/{} Begin",task.traceId(),currentExecNums,maxRetry);
		}
		executor.execute(new Runnable() {
			@Override
			public void run() {
				if(tenantId != null)CurrentRuntimeContext.setTenantId(tenantId);
				traceIdHolder.set(task.traceId());
				try {
					boolean result = task.process();
					if (!result) {
						executeWithRetry(task,currentExecNums);
					}else{
						if(task.callback != null){
							try {
								task.callback.onSuccess();
							} catch (Exception e) {
								logger.error("MENDMIX-TRACE-LOGGGING-->> {} onSuccessCallback Error:{}",task.traceId(),e.getMessage());
							}
						}
					}
					
					if(currentExecNums > 1){
						logger.info("MENDMIX-TRACE-LOGGGING-->> {} {}/{} End -> {}",task.traceId(),currentExecNums,maxRetry,result);
					}
				} catch (Exception e) {
					if(currentExecNums == maxRetry){
						logger.error(String.format("%s %s/%s Error",task.traceId(),currentExecNums,maxRetry),e);
					}else{
						logger.info("MENDMIX-TRACE-LOGGGING-->> {} {}/{} Error:{}",task.traceId(),currentExecNums,maxRetry,e.getMessage());
					}
					executeWithRetry(task,currentExecNums);
				} finally {
					traceIdHolder.remove();
					ThreadLocalContext.unset();
				}
			}
		});
	}
	

	private void onFinalErrorProcess(RetryTask task){
		try {
			if(task.callback != null){
				task.callback.onFail();
			}
		} catch (Exception e) {}
	}
	
	public Map<String, Object> status(){
		Map<String, Object> map = new HashMap<String, Object>(2);
		return map;
	}
	
	@PreDestroy
	public void shutdown(){
		try {executor.shutdown();} catch (Exception e) {}
	}
	
	public static String getTraceId(){
		return traceIdHolder.get();
	}
	
	public static void close() {
		if(defaultExecutor != null) {
			defaultExecutor.shutdown();
		}
	}
	
}
