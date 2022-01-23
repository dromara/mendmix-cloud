package com.jeesuite.logging.helper;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.util.ResourceUtils;

public class LogMessageFormat {

	private static int showLines = ResourceUtils.getInt("jeesuite.log.details.show-lines", 4);
	
	public static String buildExceptionMessages(Throwable throwable) {
		if (throwable instanceof JeesuiteBaseException == false) {
			return ExceptionUtils.getStackTrace(throwable);
		}
    	String[] traces = ExceptionUtils.getRootCauseStackTrace(throwable);
    	try {
    		StringBuilder sb = new StringBuilder();
    		showLines = showLines >= traces.length ? traces.length : showLines;
    		for (int i = 0; i < showLines; i++) {
    			sb.append(traces[i]).append("\n");
			}
    		return sb.toString();
		} catch (Exception e) {
			return ExceptionUtils.getStackTrace(e);
		}
    	
    }
}
