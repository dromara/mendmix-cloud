/**
 * 
 */
package com.jeesuite.springboot.starter.mybatis;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.jeesuite.cache.CacheExpires;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月31日
 */
@ConfigurationProperties(prefix="jeesuite.mybatis")
public class MybatisPluginProperties {

	private String dbType = "MySQL";
	private String crudDriver = "mapper3";
	private boolean cacheEnabled = false;
	private boolean rwRouteEnabled = false;
	private boolean paginationEnabled = true;
	private boolean nullValueCache = false;
	private long cacheExpireSeconds = CacheExpires.IN_1HOUR;
	private boolean dynamicExpire = false;
	private String interceptorHandlerClass;//自定义拦截器处理
	
	public boolean isCacheEnabled() {
		return cacheEnabled;
	}
	public void setCacheEnabled(boolean cacheEnabled) {
		this.cacheEnabled = cacheEnabled;
	}
	public boolean isRwRouteEnabled() {
		return rwRouteEnabled;
	}
	public void setRwRouteEnabled(boolean rwRouteEnabled) {
		this.rwRouteEnabled = rwRouteEnabled;
	}
	
	public boolean isPaginationEnabled() {
		return paginationEnabled;
	}
	public void setPaginationEnabled(boolean paginationEnabled) {
		this.paginationEnabled = paginationEnabled;
	}
	public String getDbType() {
		return dbType;
	}
	public void setDbType(String dbType) {
		this.dbType = dbType;
	}
	public String getCrudDriver() {
		return crudDriver;
	}
	public void setCrudDriver(String crudDriver) {
		this.crudDriver = crudDriver;
	}
	public boolean isNullValueCache() {
		return nullValueCache;
	}
	public void setNullValueCache(boolean nullValueCache) {
		this.nullValueCache = nullValueCache;
	}
	public long getCacheExpireSeconds() {
		return cacheExpireSeconds;
	}
	public void setCacheExpireSeconds(long cacheExpireSeconds) {
		this.cacheExpireSeconds = cacheExpireSeconds;
	}
	public boolean isDynamicExpire() {
		return dynamicExpire;
	}
	public void setDynamicExpire(boolean dynamicExpire) {
		this.dynamicExpire = dynamicExpire;
	}
	public String getInterceptorHandlerClass() {
		return interceptorHandlerClass;
	}
	public void setInterceptorHandlerClass(String interceptorHandlerClass) {
		this.interceptorHandlerClass = interceptorHandlerClass;
	}

}
