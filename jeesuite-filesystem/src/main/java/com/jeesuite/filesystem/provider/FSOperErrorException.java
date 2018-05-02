/**
 * 
 */
package com.jeesuite.filesystem.provider;

import com.jeesuite.common.JeesuiteBaseException;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月7日
 */
public class FSOperErrorException extends JeesuiteBaseException {


	private static final long serialVersionUID = 1L;
	
	private int code;
	
	public FSOperErrorException() {
	}

	public FSOperErrorException(String privoderName,Throwable cause) {
		super(9999, privoderName, cause);
	}
	
	public FSOperErrorException(String privoderName,String message) {
		this(privoderName, 500, message);
	}

	public FSOperErrorException(String privoderName,int code,String message) {
		super(9999,privoderName + "[" + message + "]");
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}
	
	
}
