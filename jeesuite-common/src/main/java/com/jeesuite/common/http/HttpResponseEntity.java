package com.jeesuite.common.http;

import java.net.HttpURLConnection;

public class HttpResponseEntity {

	private int statusCode;
	private String body;
	private Exception exception;
	
	
	
	public HttpResponseEntity() {}
	
	public HttpResponseEntity(int statusCode, String body) {
		super();
		this.statusCode = statusCode;
		this.body = body;
	}
	
	public HttpResponseEntity(int statusCode, Exception exception) {
		this.statusCode = statusCode;
		this.exception = exception;
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
	public void setBody(String body) {
		this.body = body;
	}
	
	public boolean isSuccessed(){
		return statusCode == HttpURLConnection.HTTP_OK;
	}
	
	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	@Override
	public String toString() {
		return "HttpResponseEntity [statusCode=" + statusCode + ", body=" + body + "]";
	}
	
	
	
}
