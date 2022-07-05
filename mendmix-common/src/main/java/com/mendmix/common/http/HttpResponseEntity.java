/*
 * Copyright 2016-2020 www.mendmix.com.
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
package com.mendmix.common.http;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.mendmix.common.GlobalConstants;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.model.Page;
import com.mendmix.common.util.JsonUtils;

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
	
	private static final long serialVersionUID = 1L;
	
	private static final String msgAlias = "message";
	private static final List<String> successCodes = Arrays.asList("200","0000","0");

	private int statusCode;
	private String body;
	private Map<String, String> headers;
	private String bizCode;
	private String message;
	private JsonNode bodyJsonObject;
	
	private Boolean successed;
	private Boolean isJson;
	
	
	public HttpResponseEntity() {}
	
	public HttpResponseEntity(int statusCode, String message) {
		this.statusCode = statusCode;
		this.message = message;
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
	
	public String getBizCode() {
		return bizCode;
	}

	public boolean isJson() {
		if(isJson == null) {
			isJson = body != null && JsonUtils.isJsonString(body);
		}
		return isJson;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}
	
	public void addHeader(String name,String value) {
		if(headers == null)headers = new HashMap<>();
		headers.put(name, value);
	}
	

	public JsonNode getBodyJsonObject() {
		if(bodyJsonObject == null) {
			handleBizException();
		}
		return bodyJsonObject;
	}

	public String getUnwrapBody() {
		if(!isSuccessed()) {
			throw new MendmixBaseException(getStatusCode(),getBizCode(), getMessage());
		}
		return body;
		
	}
	
	public <T> T toBean(Class<T> clazz) {
		String json = getUnwrapBody();
		if(!isJson)return null;
		return JsonUtils.toObject(json, clazz);
	}
	
	public <T> List<T> toList(Class<T> clazz) {
		String json = getUnwrapBody();
		if(!isJson)return null;
		return JsonUtils.toList(json, clazz);
	}
	
	public String toValue(String selectNode) {
		String value = JsonUtils.getJsonNodeValue(getUnwrapBody(), selectNode);
		return value;
	}
	
	public <T> T toObject(Class<T> clazz) {
		String unwrapBody = getUnwrapBody();
		if(!isJson)return null;
		String json = JsonUtils.getJsonNodeValue(unwrapBody, null);
		return JsonUtils.toObject(json, clazz);
	}
	
	public <T> List<T> toList(Class<T> clazz,String selectNode) {
		String unwrapBody = getUnwrapBody();
		if(!isJson)return null;
		String json = JsonUtils.getJsonNodeValue(unwrapBody, selectNode);
		return JsonUtils.toList(json, clazz);
	}
	
	public <T> Page<T> toPage(Class<T> clazz) {
		String json = getUnwrapBody();
		//return JsonUtils.toObject(json, new TypeReference<Page<T>>() {});
		JsonNode jsonNode = JsonUtils.selectJsonNode(json, null);
		Page<T> page = new Page<>();
		page.setPageNo(jsonNode.get("pageNo").asInt());
		page.setPageSize(jsonNode.get("pageSize").asInt());
		page.setTotal(jsonNode.get("total").asLong());
		page.setData(JsonUtils.toList(jsonNode.get("data").toString(), clazz));
		
		return page;
	}
	
	public void setBody(String body) {
		this.body = body;
	}
	
	public boolean isSuccessed(){
		handleBizException();
		return successed;
	}
	

	public String getMessage() {
		handleBizException();
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	
	public MendmixBaseException buildException() {
		return new MendmixBaseException(getStatusCode(),getBizCode(), getMessage());
	}
	
	public void appendResponseLog(StringBuilder builder) {
		builder.append("\nresponse");
		builder.append("\n - statusCode:").append(statusCode);
		if(body != null)builder.append("\n - body:").append(body);
		builder.append("\n---------------backend request trace end--------------------");
	}
	
	
	private void handleBizException() {
		if(successed != null)return;
		successed = statusCode == HttpURLConnection.HTTP_OK 
				|| (statusCode >= 200 && statusCode <= 210);
		if(successed && isJson()) {   
			bodyJsonObject = JsonUtils.toJsonNode(body);
			//
			if(!bodyJsonObject.has(GlobalConstants.PARAM_CODE)) {
				return;
			}
			//
			if(bodyJsonObject.size() > 1 && !bodyJsonObject.has(GlobalConstants.PARAM_DATA) 
					&& !bodyJsonObject.has(GlobalConstants.PARAM_MSG) 
					&& !bodyJsonObject.has(msgAlias)) {
				return;
			}
			
			String code = bodyJsonObject.get(GlobalConstants.PARAM_CODE).asText();
			if(successCodes.contains(code)) {
				bodyJsonObject = bodyJsonObject.get(GlobalConstants.PARAM_DATA);
				if(bodyJsonObject instanceof NullNode) {
					body = null;
				}else {
					body = bodyJsonObject.toString();
				}
			}else {
				successed = false;
				bizCode = bodyJsonObject.has("bizCode") ?  bodyJsonObject.get("bizCode").textValue() : null;
				if(bodyJsonObject.has(GlobalConstants.PARAM_MSG)) {
					message = bodyJsonObject.get(GlobalConstants.PARAM_MSG).textValue();
				}else {
					message = bodyJsonObject.get(msgAlias).textValue();
				}
				statusCode = StringUtils.isNumeric(code) ? Integer.parseInt(code) : 500;
			}
			
			if(!successed && message == null) {
				message = "http请求错误";
			}
		}
	}

	@Override
	public String toString() {
		return "[statusCode=" + statusCode + ", body=" + body + ", message=" + message + "]";
	}

}
