package com.jeesuite.springboot.starter;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.async.AsyncInitializer;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.logging.integrate.LogProfileManager;
import com.jeesuite.spring.ApplicationStartedListener;
import com.jeesuite.spring.InstanceFactory;

public class BaseApplicationStarter{


	protected static long before(String...args) {
		if(args != null) {
			ApplicationArguments arguments = new DefaultApplicationArguments(args);
			Set<String> optionNames = arguments.getOptionNames();
			
			for (String name : optionNames) {
				List<String> values = arguments.getOptionValues(name);
				if(values != null && !values.isEmpty()) {
					System.setProperty(name, values.get(0));
					System.out.println(String.format("add ApplicationArguments: %s = %s", name, values.get(0)));
				}
			}
		}
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

		Map<String, ApplicationStartedListener> interfaces = InstanceFactory.getBeansOfType(ApplicationStartedListener.class);
		if (interfaces != null) {
			for (ApplicationStartedListener listener : interfaces.values()) {
				System.out.println(">>>begin to execute listener:" + listener.getClass().getName());
				listener.onApplicationStarted(InstanceFactory.getContext());
				System.out.println("<<<<finish execute listener:" + listener.getClass().getName());
			}
		}
		
		//执行异步初始化
		Map<String, AsyncInitializer> asyncInitializers = InstanceFactory.getBeansOfType(AsyncInitializer.class);
			if(asyncInitializers != null){
			for (AsyncInitializer initializer : asyncInitializers.values()) {
				System.out.println(">>>begin to execute AsyncInitializer:" + initializer.getClass().getName());
				initializer.process();
			}
		}
	}
}
