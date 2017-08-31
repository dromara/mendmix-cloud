package com.jeesuite.confcenter;

import java.util.Set;

import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

public class EnvironmentChangeListener implements ApplicationListener<EnvironmentChangeEvent>, EnvironmentAware{

	private Environment environment;
	
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void onApplicationEvent(EnvironmentChangeEvent event) {
		Set<String> keys = event.getKeys();
		for (String key : keys) {
			System.out.println(">>>>>>>>>>>||||>>>>"+environment.getProperty(key));
		}
	}

}
