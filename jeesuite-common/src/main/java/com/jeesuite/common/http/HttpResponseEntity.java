package com.jeesuite.common.http;

import java.net.HttpURLConnection;

import com.jeesuite.common.json.JsonUtils;

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

	private int statusCode;
	private String body;
	private String message;
	
	
	
	public HttpResponseEntity() {}
	
	public HttpResponseEntity(int statusCode, String body) {
		super();
		this.statusCode = statusCode;
		this.body = body;
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
		return statusCode == HttpURLConnection.HTTP_OK || (statusCode >= 200 && statusCode <= 210);
	}
	

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "[statusCode=" + statusCode + ", body=" + body + ", message=" + message + "]";
	}

	
}
