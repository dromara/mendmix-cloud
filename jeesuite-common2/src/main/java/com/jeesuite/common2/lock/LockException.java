package com.jeesuite.common2.lock;

import com.jeesuite.common.JeesuiteBaseException;

public class LockException extends JeesuiteBaseException {
	private static final long serialVersionUID = 1L;

	public LockException(String e) {
		super(9999,e);
	}

	public LockException(Throwable cause) {
		super(9999, cause.getMessage(), cause);
	}
	
	
}
