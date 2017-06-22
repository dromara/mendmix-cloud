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
@ConfigurationProperties(prefix="jeesuite.cache.level1")
public class Level1CacheProperties {
	
	private boolean distributedMode = true;
	private String bcastServer;
	private String password;
	private String bcastScope;
	private String cacheProvider = "guavacache";//ehcache or guavacache
	private int maxCacheSize = 5000;
	private int timeToLiveSeconds = 300;
	private String cacheNames;//多个,隔开
	public boolean isDistributedMode() {
		return distributedMode;
	}
	public void setDistributedMode(boolean distributedMode) {
		this.distributedMode = distributedMode;
	}
	public String getBcastServer() {
		return bcastServer;
	}
	public void setBcastServer(String bcastServer) {
		this.bcastServer = bcastServer;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getBcastScope() {
		return bcastScope;
	}
	public void setBcastScope(String bcastScope) {
		this.bcastScope = bcastScope;
	}
	public String getCacheProvider() {
		return cacheProvider;
	}
	public void setCacheProvider(String cacheProvider) {
		this.cacheProvider = cacheProvider;
	}
	public int getMaxCacheSize() {
		return maxCacheSize;
	}
	public void setMaxCacheSize(int maxCacheSize) {
		this.maxCacheSize = maxCacheSize;
	}
	public int getTimeToLiveSeconds() {
		return timeToLiveSeconds;
	}
	public void setTimeToLiveSeconds(int timeToLiveSeconds) {
		this.timeToLiveSeconds = timeToLiveSeconds;
	}
	public String getCacheNames() {
		return cacheNames;
	}
	public void setCacheNames(String cacheNames) {
		this.cacheNames = cacheNames;
	}
	
	
	
}
