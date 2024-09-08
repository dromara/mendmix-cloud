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
package org.dromara.mendmix.common.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.exception.DefaultExceptions;
import org.dromara.mendmix.common.util.JsonUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * 
 * 
 * <br>
 * Class Name : WrapperResponse
 *
 * @author jiangwei
 * @version 1.0.0
 * @date Dec 16, 2016
 */
public class WrapperResponse<T> {

	// 状态
	private int code = 200;

	@JsonInclude(Include.NON_NULL)
	private String bizCode;

	// 返回信息
	@JsonInclude(Include.NON_NULL)
	private String msg;

	// 响应数据
	@JsonInclude(Include.NON_NULL)
	private T data;
	
	private Map<String, String> contextParams;

	public WrapperResponse() {
	}

	public WrapperResponse(int code, String msg) {
		super();
		this.code = code;
		this.msg = msg;
	}

	public WrapperResponse(int code, String bizCode, String msg) {
		super();
		this.code = code;
		this.bizCode = bizCode;
		this.msg = msg;
	}

	public WrapperResponse(T data) {
		this.data = data;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getBizCode() {
		return bizCode;
	}

	public void setBizCode(String bizCode) {
		this.bizCode = bizCode;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public boolean successed() {
		return code == 200;
	}
	

	public Map<String, String> getContextParams() {
		return contextParams;
	}

	public void setContextParams(Map<String, String> contextParams) {
		this.contextParams = contextParams;
	}

	@Override
	public String toString() {
		return JsonUtils.toJson(this);
	}
	
	public static WrapperResponse<Void> success(){
		return new WrapperResponse<>();
	}
	
	public static <T> WrapperResponse<T> success(T data){
		return new WrapperResponse<T>(data).withContextParam();
	}
	
	public static <T> WrapperResponse<T> fail(String msg){
		return fail(500, null, msg);
	}
	
	public static <T> WrapperResponse<T> fail(int code,String bizCode,String message){
		WrapperResponse<T> response = new WrapperResponse<>();
		response.code = code;
		response.bizCode = bizCode;
		response.msg = message;
		return response.withContextParam();
	} 
	
	public static <T> WrapperResponse<T> fail(int code,String msg){
		return fail(code, null, msg);
	}

	public static String buildErrorJSON(int code, String msg) {
		return JsonUtils.toJson(fail(code,msg));
	}

	public static <T> WrapperResponse<T> fail(Exception e) {
		MendmixBaseException be;
		if(e instanceof MendmixBaseException) {
			be  = (MendmixBaseException) e;
		}else {
			be = DefaultExceptions.SYSTEM_EXCEPTION;
		}
		return fail(be.getCode(), be.getBizCode(), be.getMessage());
	}
	
	public WrapperResponse<T> withContextParam() {
		if(contextParams != null)return this;
		contextParams = new LinkedHashMap<>(5);
		String val = CurrentRuntimeContext.getContextVal(CustomRequestHeaders.HEADER_REQUEST_ID, false);
		contextParams.put(CustomRequestHeaders.HEADER_REQUEST_ID, val);
		return this;
	}
}
