package com.jeesuite.rest.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.jeesuite.rest.filter.auth.RequestHeaderHolder;
import com.jeesuite.rest.utils.I18nUtils;

/**
 * rest 响应结果
 * 
 * @author LinHaobin
 *
 */
public class RestResponse {

	// 状态
	private int code;

	// 返回信息
	private String msg;

	// 响应数据
	@JsonInclude(Include.NON_NULL)
	private Object data;
	
	public RestResponse(){};

	/**
	 * 构造函数
	 * 
	 * @param responseCode
	 * @param msg
	 */
	public RestResponse(ResponseCodeType responseCode) {
		this.code = responseCode.getCode();
		this.msg = I18nUtils.getMessage(RequestHeaderHolder.get(),String.valueOf(code), responseCode.getMsg());
	}
	
	/**
	 * 构造函数
	 * 
	 * @param responseCode
	 * @param msg
	 */
	public RestResponse(int responseCode, String msg) {
		this.code = responseCode;
		this.msg = I18nUtils.getMessage(RequestHeaderHolder.get(),String.valueOf(code), msg);
	}

	/**
	 * 获取数据
	 * 
	 * @return
	 */
	public Object getData() {
		return data;
	}

	/**
	 * 获取状态
	 * 
	 * @return
	 */
	public int getCode() {
		return this.code;
	}

	/**
	 * 获取信息
	 * 
	 * @return
	 */
	public String getMsg() {
		return this.msg;
	}

	public void setData(Object data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "RestResponse [getData()=" + getData() + ", getCode()=" + getCode() + ", getMsg()=" + getMsg() + "]";
	}
}
