package com.jeesuite.confcenter;

import java.util.Set;

import org.springframework.context.ApplicationEvent;

public class EnvironmentChangeEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1L;
	
	private Set<String> keys;

	public EnvironmentChangeEvent(Set<String> keys) {
		super(keys);
		this.keys = keys;
	}

	/**
	 * @return the keys
	 */
	public Set<String> getKeys() {
		return keys;
	}
}
