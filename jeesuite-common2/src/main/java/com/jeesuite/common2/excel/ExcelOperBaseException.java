package com.jeesuite.common2.excel;

public class ExcelOperBaseException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ExcelOperBaseException() {}
	
	public ExcelOperBaseException(String message) {
		super(message);
	}
	
	public ExcelOperBaseException(Throwable cause) {
        super(cause);
    }

	public ExcelOperBaseException(String message, Throwable cause) {
        super(message, cause);
    }
	
}
