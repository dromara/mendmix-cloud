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
package org.dromara.mendmix.gateway.task;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.http.HttpRequestEntity;
import org.dromara.mendmix.common.model.ApiInfo;
import org.dromara.mendmix.common.util.DateUtils;
import org.dromara.mendmix.common.task.SubTimerTask;
import org.dromara.mendmix.gateway.CurrentSystemHolder;
import org.dromara.mendmix.gateway.model.BizSystem;
import org.dromara.mendmix.gateway.model.BizSystemModule;
import org.dromara.mendmix.spring.DataChangeEvent;
import org.dromara.mendmix.spring.InstanceFactory;
import org.dromara.mendmix.springweb.exporter.AppMetadataHolder;
import org.dromara.mendmix.springweb.model.AppMetadata;

public class ModuleApiRefreshTask implements SubTimerTask {

	private final static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.gateway");

	private static Date lastUpdateTime = new Date();

	@Override
	public void doSchedule() {
		List<BizSystem> systems = CurrentSystemHolder.getSystems();
		boolean changed = false;
		for (BizSystem system : systems) {
			for (BizSystemModule module : system.getModules()) {
				if (module.getApiInfos() == null || moduleMetaChanged(module)) {
					initModuleApiInfos(module);
					changed = true;
				}
			}
		}
		if(changed) {			
			InstanceFactory.getContext().publishEvent(new DataChangeEvent("moduleApis", new Object()));
		}
	}
	
	

	@Override
	public long delay() {
		return 30000;
	}



	@Override
	public long interval() {
		return 30000;
	}

	protected boolean moduleMetaChanged(BizSystemModule module) {
		if(!module.isWithMetadata())return false;
		try {
			Date currentTime = new Date();
			String value = HttpRequestEntity.get(module.getInfoUri()).backendInternalCall().execute().toValue("startTime");
			Date newUpdateTime = DateUtils.parseDate(value);
			boolean changed = newUpdateTime.after(lastUpdateTime);
			if(changed) {
				logger.info(">> service:{} version changed",module.getServiceId());
			}
			lastUpdateTime = currentTime;
			return changed;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean initModuleApiInfos(BizSystemModule module) {
		Collection<ApiInfo> apiInfos = null;
		try {
			String url;
			if(module.isGateway()) {
				apiInfos = AppMetadataHolder.getMetadata().getApis();
			} else {		
				url = module.getMetadataUri();
				if(url == null)return true;
				logger.debug("MENDMIX-TRACE-LOGGGING-->> initModuleApiInfos begin -> serviceId:{},url:{}",module.getServiceId(),url);
				apiInfos = HttpRequestEntity.get(url).execute().toObject(AppMetadata.class).getApis();
			}
		} catch (Exception e) {
			boolean ignore = e instanceof NullPointerException;
			if(!ignore && e instanceof MendmixBaseException) {
				MendmixBaseException ex = (MendmixBaseException) e;
				ignore = ex.getCode() == 404 || ex.getCode() == 401 || ex.getCode() == 403;
			}
			if(!ignore)return false;
			logger.warn("MENDMIX-TRACE-LOGGGING-->> initModuleApiInfos error -> serviceId:{},error:{}",module.getServiceId(),e.getMessage());
		}
		
		if(apiInfos != null) {
			for (ApiInfo api : apiInfos) {
				module.addApiInfo(api);
			}
			if(module.getApiInfos() == null) {
				module.setApiInfos(new HashMap<>(0));
			}
			module.updateOnApiListRefresh();
			logger.info("MENDMIX-TRACE-LOGGGING-->> initModuleApiInfos end -> serviceId:{},apiNums:{}",module.getServiceId(),module.getApiInfos().size());
		}
		
		return true;
	}

}
