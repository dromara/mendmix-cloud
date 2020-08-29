/**
 * 
 */
package com.jeesuite.springboot.starter.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月31日
 */
@ConfigurationProperties(prefix="jeesuite.cache")
public class CacheProperties {

	private String groupName;
	private String mode;
	private String servers;
	private String password;
	private int database;
	
	private int maxPoolSize;
	private int maxPoolIdle;
	private int minPoolIdle;
	private long maxPoolWaitMillis;
	
	private String masterName;
	
	private boolean tenantModeEnabled;
	
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	
	public String getServers() {
		return servers;
	}
	public void setServers(String servers) {
		this.servers = servers;
	}
	
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	public int getDatabase() {
		return database;
	}
	public void setDatabase(int database) {
		this.database = database;
	}
	
	public int getMaxPoolSize() {
		return maxPoolSize;
	}
	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}
	public int getMaxPoolIdle() {
		return maxPoolIdle;
	}
	public void setMaxPoolIdle(int maxPoolIdle) {
		this.maxPoolIdle = maxPoolIdle;
	}
	public int getMinPoolIdle() {
		return minPoolIdle;
	}
	public void setMinPoolIdle(int minPoolIdle) {
		this.minPoolIdle = minPoolIdle;
	}
	public long getMaxPoolWaitMillis() {
		return maxPoolWaitMillis;
	}
	public void setMaxPoolWaitMillis(long maxPoolWaitMillis) {
		this.maxPoolWaitMillis = maxPoolWaitMillis;
	}
	public String getMasterName() {
		return masterName;
	}
	public void setMasterName(String masterName) {
		this.masterName = masterName;
	}
	public boolean isTenantModeEnabled() {
		return tenantModeEnabled;
	}
	public void setTenantModeEnabled(boolean tenantModeEnabled) {
		this.tenantModeEnabled = tenantModeEnabled;
	}
	
	
	
}
