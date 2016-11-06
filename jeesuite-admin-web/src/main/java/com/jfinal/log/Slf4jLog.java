/**
 * 
 */
package com.jfinal.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月6日
 */
public class Slf4jLog extends Log {

	private Logger logger;
	
	public Slf4jLog(Class<?> clazz) {
		logger = LoggerFactory.getLogger(clazz);
	}
	
	public Slf4jLog(String name) {
		logger = LoggerFactory.getLogger(name);
	}

	@Override
	public void debug(String message) {
		logger.debug(message);
	}

	/* (non-Javadoc)
	 * @see com.jfinal.log.Log#debug(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void debug(String message, Throwable t) {
		logger.debug(message, t);
	}

	/* (non-Javadoc)
	 * @see com.jfinal.log.Log#info(java.lang.String)
	 */
	@Override
	public void info(String message) {
		logger.info(message);
	}

	/* (non-Javadoc)
	 * @see com.jfinal.log.Log#info(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void info(String message, Throwable t) {
		logger.info(message, t);
	}

	/* (non-Javadoc)
	 * @see com.jfinal.log.Log#warn(java.lang.String)
	 */
	@Override
	public void warn(String message) {
		logger.warn(message);
	}

	/* (non-Javadoc)
	 * @see com.jfinal.log.Log#warn(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void warn(String message, Throwable t) {
		logger.warn(message, t);
	}

	/* (non-Javadoc)
	 * @see com.jfinal.log.Log#error(java.lang.String)
	 */
	@Override
	public void error(String message) {
		logger.error(message);
	}

	/* (non-Javadoc)
	 * @see com.jfinal.log.Log#error(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void error(String message, Throwable t) {
		logger.error(message, t);
	}

	/* (non-Javadoc)
	 * @see com.jfinal.log.Log#fatal(java.lang.String)
	 */
	@Override
	public void fatal(String message) {}

	/* (non-Javadoc)
	 * @see com.jfinal.log.Log#fatal(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void fatal(String message, Throwable t) {}

	/* (non-Javadoc)
	 * @see com.jfinal.log.Log#isDebugEnabled()
	 */
	@Override
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	/* (non-Javadoc)
	 * @see com.jfinal.log.Log#isInfoEnabled()
	 */
	@Override
	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	/* (non-Javadoc)
	 * @see com.jfinal.log.Log#isWarnEnabled()
	 */
	@Override
	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	/* (non-Javadoc)
	 * @see com.jfinal.log.Log#isErrorEnabled()
	 */
	@Override
	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}

	/* (non-Javadoc)
	 * @see com.jfinal.log.Log#isFatalEnabled()
	 */
	@Override
	public boolean isFatalEnabled() {
		return logger.isErrorEnabled();
	}

}
