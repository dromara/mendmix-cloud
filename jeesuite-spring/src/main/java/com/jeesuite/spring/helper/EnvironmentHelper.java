package com.jeesuite.spring.helper;

import org.springframework.core.env.Environment;

import com.jeesuite.spring.InstanceFactory;

public class EnvironmentHelper {

	private static Environment environment;
	
	public static String getProperty(String key){
		init();
		return environment.getProperty(key);
	}

	public static void init() {
		if(environment == null){
			synchronized (EnvironmentHelper.class) {
				environment = InstanceFactory.getInstance(Environment.class);
			}
		}
	}
	
	public static boolean containsProperty(String key){
		init();
		return environment.containsProperty(key);
	}
}
