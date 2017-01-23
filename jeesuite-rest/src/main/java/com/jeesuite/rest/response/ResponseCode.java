package com.jeesuite.rest.response;

public enum ResponseCode implements HttpCodeType {
	
	OK(200, "成功"),

	BAD_REQUEST(400, "错误请求"),

	UNAUTHORIZED(401, "未授权"),

	FORBIDDEN(403, "禁止访问"),

	NOT_FOUND(404, "找不到路径"),

	METHOD_NOT_ALLOWED(405, "不允许此方法"),

	UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),
	
	NOT_ALLOW_NULL(418,"不允许为空"),
	
	NOT_ALLOWED_REPEAT(419,"不允许重复"),
	
	NO_RESOURCES(420,"资源不存在"),

	ERROR_JSON(499, "错误JSON"),

	INTERNAL_SERVER_ERROR(500, "服务器异常");

	private int code;

	private String msg;

	ResponseCode(int code, String msg) {
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

}
