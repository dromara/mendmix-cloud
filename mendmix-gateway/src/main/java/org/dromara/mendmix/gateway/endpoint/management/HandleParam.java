/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.gateway.endpoint.management;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年4月7日
 */
public class HandleParam {

	private HttpMethod httpMethod;
	private String postData;
	private MultiValueMap<String, String> queryParams;
	
	public HandleParam(ServerHttpRequest request) {
		httpMethod = request.getMethod();
		queryParams = request.getQueryParams();
		if(httpMethod == HttpMethod.POST) {
			try {
				DataBuffer dataBuffer = request.getBody().blockFirst();
				InputStream inputStream = dataBuffer.asInputStream();
				postData = new String(IOUtils.toByteArray(inputStream),StandardCharsets.UTF_8.name());
				DataBufferUtils.release(dataBuffer);
			} catch (Exception e) {}
		}
	}

	public HttpMethod getHttpMethod() {
		return httpMethod;
	}

	public String getPostData() {
		return postData;
	}
	
	public boolean isPostMethod() {
		return httpMethod == HttpMethod.POST;
	}
	
	public String getParameter(String name) {
		return queryParams.getFirst(name);
	}
}
