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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.ThreadLocalContext;

/**
 * 多租户支持定时任务抽象类
 * <br>
 * Class Name   : AbstractMultTenantJob
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年10月30日
 */
public abstract class AbstractMultTenantJob extends AbstractJob{

	private static final Logger logger = LoggerFactory.getLogger("com.mendmix.scheduler");
	@Override
	public boolean parallelEnabled() {
		return true;
	}

	@Override
	public void doJob(JobContext context) throws Exception {
		List<String> tenantIds = GlobalRuntimeContext.getTenantids();
		if(tenantIds.isEmpty()){
			doStandaloneDadaSourceJob(context);
		}else{
			for (String tenantId : tenantIds) {
				if(parallelEnabled() && !context.matchCurrentNode(tenantId)) {
					logger.debug("MENDMIX-TRACE-LOGGGING-->> doStandaloneDadaSourceJob_{} notMatchCurrentNode for:{}",getJobName(),tenantId);
					continue;
				}
				logger.debug("MENDMIX-TRACE-LOGGGING-->> doStandaloneDadaSourceJob_{} route to TenantId:{}",getJobName(),tenantId);
				CurrentRuntimeContext.setTenantId(tenantId);
				try {
					doStandaloneDadaSourceJob(context);
				} finally {
					ThreadLocalContext.unset();
				}
			}
		}
	}

	public abstract void doStandaloneDadaSourceJob(JobContext context) throws Exception;
	
}
