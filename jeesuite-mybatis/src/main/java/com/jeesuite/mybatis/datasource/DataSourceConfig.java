package com.jeesuite.mybatis.datasource;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.WebConstants;
import com.jeesuite.mybatis.MybatisConfigs;

/**
 * 
 * <br>
 * Class Name   : DataSourceConfig
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年6月30日
 */
public class DataSourceConfig {

	public static final String SLAVE_KEY = "slave";

	public static final String MASTER_KEY = "master";

	public static String DEFAULT_GROUP_NAME = "default";
	
	private String group = DEFAULT_GROUP_NAME;
	private String tenantId;
	private String url;
	private String username;
	private String password;
	private boolean  master;
	private int index;
	
	//
	private String driverClassName;
	private boolean testWhileIdle = true;
	private String validationQuery;
	private int maxActive = 20;
	private int initialSize = 1;
	private int minIdle = 1;
	private long maxWait = 15000;
	private long minEvictableIdleTimeMillis = 60000;
	private long timeBetweenEvictionRunsMillis = 60000;
	private boolean testOnBorrow = true;
	private boolean testOnReturn = false;
	
	
	public String getGroup() {
		return group;
	}
	public void setGroup(String group) {
		this.group = group;
	}
	public String getTenantId() {
		return tenantId;
	}
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public boolean isMaster() {
		return master;
	}
	public void setMaster(boolean master) {
		this.master = master;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	
	public String getDriverClassName() {
		return driverClassName;
	}
	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}
	public boolean isTestWhileIdle() {
		return testWhileIdle;
	}
	public void setTestWhileIdle(boolean testWhileIdle) {
		this.testWhileIdle = testWhileIdle;
	}
	public String getValidationQuery() {
		return validationQuery;
	}
	public void setValidationQuery(String validationQuery) {
		this.validationQuery = validationQuery;
	}
	public int getMaxActive() {
		return maxActive;
	}
	public void setMaxActive(int maxActive) {
		this.maxActive = maxActive;
	}
	public int getInitialSize() {
		return initialSize;
	}
	public void setInitialSize(int initialSize) {
		this.initialSize = initialSize;
	}
	public int getMinIdle() {
		return minIdle;
	}
	public void setMinIdle(int minIdle) {
		this.minIdle = minIdle;
	}
	public long getMaxWait() {
		return maxWait;
	}
	public void setMaxWait(long maxWait) {
		this.maxWait = maxWait;
	}
	public long getMinEvictableIdleTimeMillis() {
		return minEvictableIdleTimeMillis;
	}
	public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
		this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
	}
	public long getTimeBetweenEvictionRunsMillis() {
		return timeBetweenEvictionRunsMillis;
	}
	public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
		this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
	}
	public boolean isTestOnBorrow() {
		return testOnBorrow;
	}
	public void setTestOnBorrow(boolean testOnBorrow) {
		this.testOnBorrow = testOnBorrow;
	}
	public boolean isTestOnReturn() {
		return testOnReturn;
	}
	public void setTestOnReturn(boolean testOnReturn) {
		this.testOnReturn = testOnReturn;
	}
	public String dataSourceKey() {
		return buildDataSourceKey(group, tenantId, master, index);
	}
	
	public void validate() {
		if(StringUtils.isAnyBlank(url,username)) {
			throw new JeesuiteBaseException("DataSourceConfig[url,username,password] is required");
		}
		//租户分库
		if(StringUtils.isBlank(tenantId) && MybatisConfigs.isSchameSharddingTenant(group)) {
			throw new JeesuiteBaseException("DataSourceConfig[tenantId] is required For SchameSharddingTenant");
		}
	}
	
	@Override
	public String toString() {
		return "DataSourceConfig [group=" + group + ", tenantId=" + tenantId + ", url=" + url + ", username=" + username
				+ ", master=" + master + ", index=" + index + ", maxActive=" + maxActive + "]";
	}
	
	public static String buildDataSourceKey(String group,String tenantId,boolean master,int index) {
		StringBuilder builder = new StringBuilder(group).append(WebConstants.UNDER_LINE);
		if(tenantId != null) {
			builder.append(tenantId).append(WebConstants.UNDER_LINE);
		}
		builder.append(master ? MASTER_KEY : SLAVE_KEY).append(WebConstants.UNDER_LINE);
		builder.append(index);
		return builder.toString();
	}
	
	public static void main(String[] args) {
		for (int i = 0; i < 1000; i++) {
			System.out.println(RandomUtils.nextInt(0, 5));
		}
	}
	
}
