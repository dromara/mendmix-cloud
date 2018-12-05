package com.jeesuite.security.exception;

import com.jeesuite.common.JeesuiteBaseException;

public class UserPasswordWrongException extends JeesuiteBaseException {

	private static final long serialVersionUID = 1L;
	
	private static final String MSG = "密码错误";

	public UserPasswordWrongException() {
		super(4001,MSG);
	}
	
	

}
