package com.jeesuite.scheduler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.ThreadLocalContext;

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

	private static final Logger logger = LoggerFactory.getLogger("com.jeesuite.scheduler");
	@Override
	public boolean parallelEnabled() {
		return false;
	}

	@Override
	public void doJob(JobContext context) throws Exception {
		List<String> tenantIds = GlobalRuntimeContext.getTenantids();
		if(tenantIds.isEmpty()){
			doStandaloneDadaSourceJob(context);
		}else{
			for (String tenantId : tenantIds) {
				logger.debug("doStandaloneDadaSourceJob route to TenantId:{}",tenantId);
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
