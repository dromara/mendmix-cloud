/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.security.event;

import org.springframework.context.ApplicationEvent;

import com.mendmix.security.model.UserSession;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Nov 19, 2022
 */
public class SessionLifeCycleEvent extends ApplicationEvent{

	private static final long serialVersionUID = 1L;
	
	private SessionEventType eventType;
	
	public SessionLifeCycleEvent(SessionEventType eventType,UserSession session) {
		super(session);
		this.eventType = eventType;
	}

	public SessionEventType getEventType() {
		return eventType;
	}

	@Override
	public UserSession getSource() {
		return (UserSession) super.getSource();
	}
	
	

}
