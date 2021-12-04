package com.jeesuite.springweb.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.jeesuite.common.util.JsonUtils;

/**
 * 
 * 
 * <br>
 * Class Name   : WrapperResponse
 *
 * @author jiangwei
 * @version 1.0.0
 * @date Dec 16, 2016
 */
public class WrapperResponse<T> {

	private static String ERROR_JSON_TEMPLATE = "{\"code\": %s,\"msg\":\"%s\"}";
	
	// 状态
	private int code = 200;

	// 返回信息
	@JsonInclude(Include.NON_NULL)
	private String msg;

	// 响应数据
	@JsonInclude(Include.NON_NULL)
	private T data;
	
	public WrapperResponse(){}
	
	public WrapperResponse(int code, String msg) {
		super();
		this.code = code;
		this.msg = msg;
	}
	
	public WrapperResponse(int code, String msg, T data) {
		this.code = code;
		this.msg = msg;
		this.data = data;
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
	
	public boolean successed(){
		return code == 200;
	}

	@Override
	public String toString() {
		return JsonUtils.toJson(this);
	}
	
	public static String buildErrorJSON(int code,String msg) {
		return String.format(ERROR_JSON_TEMPLATE, code,msg);
	}
}
