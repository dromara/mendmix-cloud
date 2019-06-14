package com.jeesuite.log;

import java.lang.reflect.Constructor;

public class LogFactory {

	private static Constructor<?> logConstructor;
	
	static{
		Class<?> implClass = null;
		try {
            classForName("org.apache.log4j.Logger");
            implClass = classForName("com.ayg.openapi.log.Log4jImpl");
        } catch (Throwable t) {}
		
		if(implClass == null){
			try {
	            classForName("org.apache.logging.log4j.Logger");
	            implClass = classForName("com.ayg.openapi.log.Log4j2Impl");
	        } catch (Throwable t) {}
		}
		
		if(implClass == null){
			try {
	            classForName("org.slf4j.Logger");
	            implClass = classForName("com.ayg.openapi.log.SLF4JImpl");
	        } catch (Throwable t) {}
		}
		
		try {			
			logConstructor = implClass.getConstructor(new Class[] { String.class });
		} catch (Throwable e) {}
	}
	
	public static Log getLog(String loggerName) {
        try {
            return (Log) logConstructor.newInstance(loggerName);
        } catch (Throwable t) {
            throw new RuntimeException("Error creating logger for logger '" + loggerName + "'.  Cause: " + t, t);
        }
    }
	
	private static Class<?> classForName(String className) throws ClassNotFoundException {
        Class<?> clazz = null;
        try {
            clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (Exception e) {
            // Ignore. Failsafe below.
        }
        if (clazz == null) {
            clazz = Class.forName(className);
        }
        return clazz;
    }
}
