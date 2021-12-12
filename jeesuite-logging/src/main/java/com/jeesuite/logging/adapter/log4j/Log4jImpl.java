/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.jeesuite.logging.adapter.log4j;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.jeesuite.logging.AbstractLogger;

/**
 * @author Eduardo Macarron
 */
public class Log4jImpl extends AbstractLogger {

	private static final String FQCN = Log4jImpl.class.getName();

	private final Logger log;

	public Log4jImpl(String clazz) {
		log = Logger.getLogger(clazz);
	}

	@Override
	public boolean isDebugEnabled() {
		return log.isDebugEnabled();
	}

	@Override
	public boolean isTraceEnabled() {
		return log.isTraceEnabled();
	}

	@Override
	public void error(String s, Throwable e) {
		log.log(FQCN, Level.ERROR, s, e);
	}

	@Override
	public void error(String s) {
		log.log(FQCN, Level.ERROR, s, null);
	}

	@Override
	public void debug(String s) {
		log.log(FQCN, Level.DEBUG, s, null);
	}

	@Override
	public void trace(String s) {
		log.log(FQCN, Level.TRACE, s, null);
	}

	@Override
	public void warn(String s) {
		log.log(FQCN, Level.WARN, s, null);
	}

	@Override
	public boolean isInfoEnabled() {
		return log.isInfoEnabled();
	}

	@Override
	public void info(String s) {
		log.log(FQCN, Level.INFO, s, null);
	}

}
