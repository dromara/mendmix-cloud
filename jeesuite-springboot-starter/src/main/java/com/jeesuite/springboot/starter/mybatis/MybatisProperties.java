/**
 * 
 */
package com.jeesuite.springboot.starter.mybatis;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月31日
 */
@ConfigurationProperties(prefix="jeesuite.mybatis")
public class MybatisProperties {

	
	private String typeAliasesPackage;
	private String mapperLocations;
	private String mapperBasePackage;
	private int database;
	
	private int maxPoolSize;
	private int maxPoolIdle;
	private int minPoolIdle;
	private long maxPoolWaitMillis;
	
	private String masterName;
	
	
}
