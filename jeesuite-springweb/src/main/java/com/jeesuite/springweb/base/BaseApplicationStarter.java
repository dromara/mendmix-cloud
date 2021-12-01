package com.jeesuite.springweb.base;

import java.util.Map;

import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.async.AsyncInitializer;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.spring.ApplicationStartedListener;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.springweb.logging.LogProfileManager;

public class BaseApplicationStarter{


	protected static long before() {
		LogProfileManager.initialize();
		System.setProperty("client.nodeId", GlobalRuntimeContext.getNodeName());
		return System.currentTimeMillis();
	}

	protected static void after(long starTime) {
        //
		LogProfileManager.reload();
		
		long endTime = System.currentTimeMillis();
		long time = endTime - starTime;
		System.out.println("\nStart Time: " + time / 1000 + " s");
		System.out.println("...............................................................");
		System.out.println("..................Service starts successfully (port:"+ResourceUtils.getProperty("server.port")+")..................");
		System.out.println("...............................................................");

		Map<String, ApplicationStartedListener> interfaces = InstanceFactory.getInstanceProvider()
				.getInterfaces(ApplicationStartedListener.class);
		if (interfaces != null) {
			for (ApplicationStartedListener listener : interfaces.values()) {
				System.out.println(">>>begin to execute listener:" + listener.getClass().getName());
				listener.onApplicationStarted(InstanceFactory.getContext());
				System.out.println("<<<<finish execute listener:" + listener.getClass().getName());
			}
		}
		
		//执行异步初始化
		Map<String, AsyncInitializer> asyncInitializers = InstanceFactory.getInstanceProvider().getInterfaces(AsyncInitializer.class);
			if(asyncInitializers != null){
			for (AsyncInitializer initializer : asyncInitializers.values()) {
				System.out.println(">>>begin to execute AsyncInitializer:" + initializer.getClass().getName());
				initializer.process();
			}
		}
	}
}
