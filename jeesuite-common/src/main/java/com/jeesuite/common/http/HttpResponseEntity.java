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
package com.jeesuite.common.http;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.jeesuite.common.GlobalConstants;
import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.model.Page;
import com.jeesuite.common.util.JsonUtils;

/**
 * 
 * 
 * <br>
 * Class Name   : HttpResponseEntity
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Apr 29, 2021
 */
public class HttpResponseEntity {
	
	private static final String msgAlias = "message";
	private static final List<String> successCodes = Arrays.asList("200","0000","0");

	private int statusCode;
	private String body;
	private String message;
	private boolean jsonBody;
	
	
	
	public HttpResponseEntity() {}
	
	public HttpResponseEntity(int statusCode, String body) {
		this.statusCode = statusCode;
		setBody(body);
	}


	public int getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	public String getBody() {
		return body;
	}
	
	public String getUnwrapBody() {
		jsonBody = body != null && JsonUtils.isJsonString(body);
		if(jsonBody) {
			JsonNode jsonNode = JsonUtils.getNode(body, null);
			//
			if(!jsonNode.has(GlobalConstants.PARAM_CODE)) {
				return body;
			}
			//
			if(jsonNode.size() > 1 && !jsonNode.has(GlobalConstants.PARAM_DATA) 
					&& !jsonNode.has(GlobalConstants.PARAM_MSG) 
					&& !jsonNode.has(msgAlias)) {
				return body;
			}
			
			String code = jsonNode.get(GlobalConstants.PARAM_CODE).asText();
			if(successCodes.contains(code)) {
				JsonNode dataNode = jsonNode.get(GlobalConstants.PARAM_DATA);
				if(dataNode instanceof NullNode) {
					return null;
				}
				return Objects.toString(dataNode, null);
			}
			String bizCode = jsonNode.has("bizCode") ?  jsonNode.get("bizCode").textValue() : null;
			String msg = null;
			if(jsonNode.has(GlobalConstants.PARAM_MSG)) {
				msg = jsonNode.get(GlobalConstants.PARAM_MSG).textValue();
			}else {
				msg = jsonNode.get(msgAlias).textValue();
			}
			int statusCode = StringUtils.isNumeric(code) ? Integer.parseInt(code) : 500;
 			throw new JeesuiteBaseException(statusCode, bizCode, msg);
		}
		
		if(!isSuccessed()) {
			throw new JeesuiteBaseException(statusCode, StringUtils.defaultIfBlank(message, "http请求异常"));
		}
		
		return body;
		
	}
	
	public <T> T toObject(Class<T> clazz) {
		String json = getUnwrapBody();
		if(!jsonBody)return null;
		return JsonUtils.toObject(json, clazz);
	}
	
	public <T> List<T> toList(Class<T> clazz) {
		String json = getUnwrapBody();
		if(!jsonBody)return null;
		return JsonUtils.toList(json, clazz);
	}
	
	public String toValue(String selectNode) {
		String json = getUnwrapBody();
		if(!jsonBody)return null;
		String value = JsonUtils.getJsonNodeValue(json, selectNode);
		return value;
	}
	
	public <T> T toObject(Class<T> clazz,String selectNode) {
		String json = getUnwrapBody();
		if(!jsonBody)return null;
		json = JsonUtils.getJsonNodeValue(json, selectNode);
		return JsonUtils.toObject(json, clazz);
	}
	
	public <T> List<T> toList(Class<T> clazz,String selectNode) {
		String json = getUnwrapBody();
		if(!jsonBody)return null;
		json = JsonUtils.getJsonNodeValue(json, selectNode);
		return JsonUtils.toList(json, clazz);
	}
	
	public <T> Page<T> toPage(Class<T> clazz) {
		String json = getUnwrapBody();
		if(!jsonBody)return null;
		return JsonUtils.toObject(json, new TypeReference<Page<T>>() {});
	}
	
	public void setBody(String body) {
		this.body = body;
	}
	
	public boolean isSuccessed(){
		boolean success = statusCode == HttpURLConnection.HTTP_OK 
				|| (statusCode >= 200 && statusCode <= 210);
		return success;
	}
	

	public String getMessage() {
		return StringUtils.trimToEmpty(message);
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "[statusCode=" + statusCode + ", body=" + body + ", message=" + message + "]";
	}

}
