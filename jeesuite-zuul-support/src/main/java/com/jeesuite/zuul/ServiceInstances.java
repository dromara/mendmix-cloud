package com.jeesuite.zuul;

import com.jeesuite.spring.InstanceFactory;

public class ServiceInstances {

	private static SystemMgrApi systemMgrApi;
	
	public static SystemMgrApi systemMgrApi() {
		if(systemMgrApi != null)return systemMgrApi;
		synchronized (CurrentSystemHolder.class) {
			systemMgrApi = InstanceFactory.getInstance(SystemMgrApi.class);
		}
		return systemMgrApi;
	}

}
