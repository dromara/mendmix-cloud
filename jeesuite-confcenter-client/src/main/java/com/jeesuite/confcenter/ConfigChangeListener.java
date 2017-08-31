package com.jeesuite.confcenter;

public interface ConfigChangeListener {

	void register(ConfigcenterContext context);
	
	void unRegister();
	
	String typeName();
}
