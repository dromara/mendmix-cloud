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
package org.dromara.mendmix.spring.autoconfigure;

import java.util.Map;
import java.util.concurrent.RejectedExecutionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.ThreadLocalContext;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Jun 17, 2023
 */
@Configuration
public class AsyncTaskExecutorConfigurer implements AsyncConfigurer {

	private final static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix");

	@Value("${spring.customize.asyncExecutor.corePoolSize:1}")
	private int corePoolSize;
	@Value("${spring.customize.asyncExecutor.maxPoolSize:20}")
	private int maxPoolSize;
	@Value("${spring.customize.asyncExecutor.queueCapacity:1000}")
	private int queueCapacity;

	@Override
	public ThreadPoolTaskExecutor getAsyncExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setThreadNamePrefix("springAsyncTask-");
		taskExecutor.setCorePoolSize(corePoolSize);
		taskExecutor.setMaxPoolSize(maxPoolSize);
		taskExecutor.setQueueCapacity(queueCapacity);
		taskExecutor.setRejectedExecutionHandler(REJECT_HANDLER);
		taskExecutor.setTaskDecorator(TASK_DECORATOR);
		taskExecutor.setTaskDecorator(TASK_DECORATOR);
		taskExecutor.initialize();
		return taskExecutor;
	}

	private static RejectedExecutionHandler REJECT_HANDLER = (r, e) -> {
		String name = Thread.currentThread().getName();
		logger.warn(">>AsyncTaskExecutor Rejected for:{},status:{}",name,e.toString());
		throw new MendmixBaseException("异步任务队列已满");
	};

	private static TaskDecorator TASK_DECORATOR = runnable -> {
		// 复制父线程的上下文
		Map<String, String> allContextVars = CurrentRuntimeContext.getContextHeaders();
		return () -> {
			try {
				logger.debug(">>AsyncTaskExecutor CurrentRuntimeContext: {}", allContextVars);
				allContextVars.forEach(ThreadLocalContext::set);
				runnable.run();
			} finally {
				ThreadLocalContext.unset();
			}
		};
	};
}

