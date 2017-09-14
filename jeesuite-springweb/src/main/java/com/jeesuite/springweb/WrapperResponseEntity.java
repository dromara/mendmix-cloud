package com.jeesuite.springweb;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


public class WrapperResponseEntity {

	// 状态
	private int code = 200;

	// 返回信息
	private String msg;

	// 响应数据
	@JsonInclude(Include.NON_NULL)
	private Object data;
	
	public WrapperResponseEntity(){}
	
	public WrapperResponseEntity(int code, String msg) {
		super();
		this.code = code;
		this.msg = msg;
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

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "RestResponse [getData()=" + getData() + ", getCode()=" + getCode() + ", getMsg()=" + getMsg() + "]";
	}
}
