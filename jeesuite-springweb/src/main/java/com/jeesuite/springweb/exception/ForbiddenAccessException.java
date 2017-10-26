package com.jeesuite.springweb.exception;

import com.jeesuite.common.JeesuiteBaseException;

public class ForbiddenAccessException extends JeesuiteBaseException {

	private static final long serialVersionUID = 1L;
	
	private static final String MSG = "403 forbidden";

	public ForbiddenAccessException() {
		super(403,MSG);
	}
	
	

}
