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
	private String baseMapperClass;
	private boolean cacheEnabled = false;
	private boolean rwRouteEnabled = false;
	private boolean dbShardEnabled = false;
	private boolean paginationEnabled = false;
	
	public String getTypeAliasesPackage() {
		return typeAliasesPackage;
	}
	public void setTypeAliasesPackage(String typeAliasesPackage) {
		this.typeAliasesPackage = typeAliasesPackage;
	}
	public String getMapperLocations() {
		return mapperLocations;
	}
	public void setMapperLocations(String mapperLocations) {
		this.mapperLocations = mapperLocations;
	}
	public String getMapperBasePackage() {
		return mapperBasePackage;
	}
	public void setMapperBasePackage(String mapperBasePackage) {
		this.mapperBasePackage = mapperBasePackage;
	}
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
	public boolean isDbShardEnabled() {
		return dbShardEnabled;
	}
	public void setDbShardEnabled(boolean dbShardEnabled) {
		this.dbShardEnabled = dbShardEnabled;
	}
	
	public boolean isPaginationEnabled() {
		return paginationEnabled;
	}
	public void setPaginationEnabled(boolean paginationEnabled) {
		this.paginationEnabled = paginationEnabled;
	}
	public String getBaseMapperClass() {
		return baseMapperClass;
	}
	public void setBaseMapperClass(String baseMapperClass) {
		this.baseMapperClass = baseMapperClass;
	}

}
