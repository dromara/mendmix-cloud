package com.jeesuite.common;

public class JeesuiteBaseException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private int code;
	
	public JeesuiteBaseException() {
		super();
	}
	
	public JeesuiteBaseException(String message) {
		this(500, message);
	}

	public JeesuiteBaseException(int code,String message) {
		super(message);
		this.code = code;
	}
	
	public JeesuiteBaseException(int code,String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}
	
	

}
