package com.jeesuite.springweb.model;

public class WrapperResponseEntity extends WrapperResponse<Object> {

	public WrapperResponseEntity() {
		super();
	}

	public WrapperResponseEntity(int code, String msg, Object data) {
		super(code, msg, data);
	}

	public WrapperResponseEntity(int code, String msg) {
		super(code, msg);
	}

	public WrapperResponseEntity(Object data) {
		super(data);
	}
	
}
