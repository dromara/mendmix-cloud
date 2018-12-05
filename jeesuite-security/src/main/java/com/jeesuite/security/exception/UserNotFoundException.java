package com.jeesuite.security.exception;

import com.jeesuite.common.JeesuiteBaseException;

public class UserNotFoundException extends JeesuiteBaseException {

	private static final long serialVersionUID = 1L;
	
	private static final String MSG = "用户不存在";

	public UserNotFoundException() {
		super(4001,MSG);
	}
	
	

}
