/*
 * Copyright 2016-2020 www.jeesuite.com.
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
package com.jeesuite.springboot.autoconfigure.loadbalancer;

import java.net.URI;
import java.util.Map;

import org.springframework.cloud.client.ServiceInstance;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年5月21日
 */
public class FixedServiceInstance implements ServiceInstance {

	private String serviceId;
	private String host;
	private int port; 
	private URI uri;
	private boolean secure;
	
	
	public FixedServiceInstance(String serviceId,URI uri) {
		this.serviceId = serviceId;
		this.uri = uri;
		this.host = uri.getHost();
		this.port = uri.getPort();
		this.secure = "https".equals(uri.getScheme());
	}
	
	@Override
	public String getServiceId() {
		return serviceId;
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public boolean isSecure() {
		return secure;
	}

	@Override
	public URI getUri() {
		return uri;
	}

	@Override
	public Map<String, String> getMetadata() {
		return null;
	}

	@Override
	public String toString() {
		return "[serviceId=" + serviceId + ", host=" + host + ", port=" + port + ", secure=" + secure + "]";
	}
}
