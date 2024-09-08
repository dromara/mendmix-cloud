package org.dromara.mendmix.spring;

import org.springframework.context.ApplicationEvent;


public class CommonApplicationEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1L;
	
	private SpringEventType eventType;
	
	public CommonApplicationEvent(SpringEventType eventType,Object source) {
		super(source);
		this.eventType = eventType;
	}

	public SpringEventType getEventType() {
		return eventType;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getEventData() {
		return (T) getSource();
	}

}
