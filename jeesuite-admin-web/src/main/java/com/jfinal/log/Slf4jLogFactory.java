/**
 * 
 */
package com.jfinal.log;

public class Slf4jLogFactory implements ILogFactory {

	@Override
	public Log getLog(Class<?> clazz) {
		return new Slf4jLog(clazz);
	}

	@Override
	public Log getLog(String name) {
		return new Slf4jLog(name);
	}
	
}
