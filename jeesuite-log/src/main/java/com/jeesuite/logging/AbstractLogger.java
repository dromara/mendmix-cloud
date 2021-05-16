package com.jeesuite.logging;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractLogger implements Logger {

	
	private static final String PLACE_HOLDER = "{}";

	@Override
	public void debug(String format, Object... arguments) {
		debug(formatMessage(format, arguments));
	}

	@Override
	public void trace(String format, Object... arguments) {
		trace(formatMessage(format, arguments));
	}

	@Override
	public void warn(String format, Object... arguments) {
		warn(formatMessage(format, arguments));
	}


	@Override
	public void info(String format, Object... arguments) {
		info(formatMessage(format, arguments));
	}
	
	private static String formatMessage(String format, Object... arguments) {
		if(arguments == null || arguments.length == 0)return format;
		
		String message = format;
		for (Object arg : arguments) {
			message = StringUtils.replaceOnce(message, PLACE_HOLDER, arg.toString());
		}
		return message;
	}

}
