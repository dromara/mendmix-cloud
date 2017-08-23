package com.jeesuite.common2.lock;

public class LockException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public LockException(String e) {
		super(e);
	}

	public LockException(Exception e) {
		super(e);
	}
}
