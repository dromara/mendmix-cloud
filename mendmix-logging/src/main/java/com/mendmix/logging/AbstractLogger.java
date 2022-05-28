/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.logging;

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
