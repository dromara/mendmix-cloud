package com.jeesuite.springweb.exception;

import com.jeesuite.common.JeesuiteBaseException;

public class UnauthorizedException extends JeesuiteBaseException {

	private static final long serialVersionUID = 1L;
	
	private static final String MSG = "401 Unauthorized";

	public UnauthorizedException() {
		super(401,MSG);
	}
	
	

}
