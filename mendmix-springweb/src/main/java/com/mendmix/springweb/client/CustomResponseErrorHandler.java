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
package com.mendmix.springweb.client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import com.google.common.io.CharStreams;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.util.JsonUtils;

/**
 * 全局错误处理hanlder
 * 
 * <br>
 * Class Name   : CustomResponseErrorHandler
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年10月17日
 */
public class CustomResponseErrorHandler extends DefaultResponseErrorHandler {
	/**
	 * 
	 */
	private static final String DEFAULT_ERROR_MSG = "invoke remote service error";

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		int code = response.getRawStatusCode();
		String content = CharStreams.toString(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8));
		
		Map<?, ?> responseItmes = null;
		if(code == 404 && StringUtils.isNotBlank(content)){
			responseItmes = JsonUtils.toObject(content, Map.class);
			throw new MendmixBaseException(404, "Page Not Found["+responseItmes.get("path")+"]");
		}

		int errorCode = 500;
		String errorMsg = content;
		try {responseItmes = JsonUtils.toObject(content, Map.class);} catch (Exception e) {}
		if(responseItmes != null){
			if(responseItmes.containsKey("code")){
				errorCode = Integer.parseInt(responseItmes.get("code").toString());
			}
			if(responseItmes.containsKey("msg")){
				errorMsg = responseItmes.get("msg").toString();
			}else if(responseItmes.containsKey("message")){
				errorMsg = responseItmes.get("message").toString();
			}
		}
		
		if(StringUtils.isBlank(errorMsg)){
			errorMsg = DEFAULT_ERROR_MSG;
		}
		
		throw new MendmixBaseException(errorCode, errorMsg + "(Remote)");
	}

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return response.getStatusCode().isError();
	}
}
