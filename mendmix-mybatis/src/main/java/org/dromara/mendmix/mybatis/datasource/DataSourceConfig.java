/*
 * Copyright 2016-2020 dromara.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dromara.mendmix.mybatis.datasource;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.util.ResourceUtils;
import org.dromara.mendmix.mybatis.MybatisConfigs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * 
 * <br>
 * Class Name   : DataSourceConfig
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年6月30日
 */
@JsonInclude(Include.NON_NULL)
public class DataSourceConfig {

	private static final String DEFAULT_DRIVER_CLASS_NAME = ResourceUtils.getProperty("db.driverClassName", "com.mysql.cj.jdbc.Driver");
	private static final int DEFAULT_MAX_ACTIVE = ResourceUtils.getInt("db.maxActive", 20);
	private static final boolean DEFAULT_KEEP_ALIVE = ResourceUtils.getBoolean("db.keepAlive", false);
	private static final boolean DEFAULT_TEST_ON_RETUEN = ResourceUtils.getBoolean("db.testOnReturn", false);
	private static final boolean DEFAULT_TEST_ON_BORROW = ResourceUtils.getBoolean("db.testOnBorrow", true);
	private static final long DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MS = ResourceUtils.getLong("db.timeBetweenEvictionRunsMillis",60000);
	private static final long DEFAULT_MIN_EVICTABLE_IDLE_TIME_MS = ResourceUtils.getLong("db.minEvictableIdleTimeMillis",60000);
	private static final long DEFAULT_KEEP_ALIVE_BETWEEN_TIME_MS = ResourceUtils.getLong("db.keepAliveBetweenTimeMillis");
	private static final long DEFAULT_MAX_WAIT = ResourceUtils.getLong("db.maxWait",30000);
	private static final int DEFAULT_MIN_IDLE = ResourceUtils.getInt("db.minIdle", 1);
	private static final int DEFAULT_INITIAL_SIZE = ResourceUtils.getInt("db.initialSize", 1);
	private static final String DEFAULT_VALIDATION_QUERY = ResourceUtils.getProperty("db.validationQuery", "SELECT 'x'");
	private static final boolean DEFAULT_TEST_WHILE_IDLE = ResourceUtils.getBoolean("db.testWhileIdle", true);
	private static final int DEFAULT_CONN_TIMEOUT = ResourceUtils.getInt("db.connectTimeout", 10000);
	private static final int DEFAULT_SOCKET_TIMEOUT = ResourceUtils.getInt("db.socketTimeout", 10000);

	private static final boolean DEFAULT_REMOVE_ABANDONED = ResourceUtils.getBoolean("db.removeAbandoned", false);
	private static final long DEFAULT_REMOVE_ABANDONED__TIMEOUT_MS = ResourceUtils.getLong("db.removeAbandonedTimeoutMillis",300 * 1000);
	private static final boolean DEFAULT_ABANDONED_LOGGING = ResourceUtils.getBoolean("db.logAbandoned", false);
	
	public static final String SLAVE_KEY = "slave";

	public static final String MASTER_KEY = "master";

	public static String DEFAULT_GROUP_NAME = "default";
	
	private String id;
	private String group = DEFAULT_GROUP_NAME;
	private String url;
	private String username;
	private String password;
	private boolean  master;
	private int index;
	
	private String hostAndPort;
	
	//
	private String driverClassName = DEFAULT_DRIVER_CLASS_NAME;
	private boolean testWhileIdle = DEFAULT_TEST_WHILE_IDLE;
	private String validationQuery = DEFAULT_VALIDATION_QUERY;
	private int maxActive = DEFAULT_MAX_ACTIVE;
	private int initialSize = DEFAULT_INITIAL_SIZE;
	private int minIdle = DEFAULT_MIN_IDLE;
	private long maxWait = DEFAULT_MAX_WAIT;
	private long minEvictableIdleTimeMillis = DEFAULT_MIN_EVICTABLE_IDLE_TIME_MS;
	private long timeBetweenEvictionRunsMillis = DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MS;
	private boolean testOnBorrow = DEFAULT_TEST_ON_BORROW;
	private boolean testOnReturn = DEFAULT_TEST_ON_RETUEN;
	private boolean keepAlive = DEFAULT_KEEP_ALIVE;
	private long keepAliveBetweenTimeMillis = DEFAULT_KEEP_ALIVE_BETWEEN_TIME_MS > 0 ? DEFAULT_KEEP_ALIVE_BETWEEN_TIME_MS : timeBetweenEvictionRunsMillis + 5000;
	private int connectTimeout = DEFAULT_CONN_TIMEOUT; // milliSeconds
	private int socketTimeout = DEFAULT_SOCKET_TIMEOUT; 
	private boolean removeAbandoned = DEFAULT_REMOVE_ABANDONED;
	private long removeAbandonedTimeoutMillis = DEFAULT_REMOVE_ABANDONED__TIMEOUT_MS;
	private boolean logAbandoned = DEFAULT_ABANDONED_LOGGING;
	
	private String tenantRouteKey;
	private Set<String> scopeTenantIds;
	
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getGroup() {
		return group;
	}
	public void setGroup(String group) {
		this.group = group;
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
	
	public String getHostAndPort() {
		if(hostAndPort == null) {
			hostAndPort = StringUtils.split(url, "/")[1];
		}
		return hostAndPort;
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
	public boolean isKeepAlive() {
		return keepAlive;
	}
	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}
	public long getKeepAliveBetweenTimeMillis() {
		return keepAliveBetweenTimeMillis;
	}
	public void setKeepAliveBetweenTimeMillis(long keepAliveBetweenTimeMillis) {
		this.keepAliveBetweenTimeMillis = keepAliveBetweenTimeMillis;
	}
	
	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}
	
	public boolean isRemoveAbandoned() {
		return removeAbandoned;
	}

	public void setRemoveAbandoned(boolean removeAbandoned) {
		this.removeAbandoned = removeAbandoned;
	}

	public long getRemoveAbandonedTimeoutMillis() {
		return removeAbandonedTimeoutMillis;
	}

	public void setRemoveAbandonedTimeoutMillis(long removeAbandonedTimeoutMillis) {
		this.removeAbandonedTimeoutMillis = removeAbandonedTimeoutMillis;
	}

	public boolean isLogAbandoned() {
		return logAbandoned;
	}

	public void setLogAbandoned(boolean logAbandoned) {
		this.logAbandoned = logAbandoned;
	}

	public String getTenantRouteKey() {
		return tenantRouteKey;
	}
	
	public void setTenantRouteKey(String tenantRouteKey) {
		this.tenantRouteKey = tenantRouteKey;
	}
	
	public Set<String> getScopeTenantIds() {
		return scopeTenantIds != null ? scopeTenantIds : (scopeTenantIds = new HashSet<>());
	}
	
	public void setScopeTenantIds(Set<String> scopeTenantIds) {
		this.scopeTenantIds = scopeTenantIds;
	}
	
	public void addScopeTenantId(String tenantId) {
		getScopeTenantIds().add(tenantId);
	}
	
	public String dataSourceKey() {
		return buildDataSourceKey(group, tenantRouteKey, master, index);
	}
	
	public void validate() {
		if(StringUtils.isAnyBlank(url,username)) {
			throw new MendmixBaseException("DataSourceConfig[url,username,password] is required");
		}
		//租户分库:兼容按库隔离和字段隔离模式
		if(StringUtils.isBlank(tenantRouteKey) 
				&& StringUtils.isBlank(MybatisConfigs.getTenantColumnName(group)) 
				&& MybatisConfigs.isSchameSharddingTenant(group)) {
			throw new MendmixBaseException("DataSourceConfig[tenantId] is required For SchameSharddingTenant");
		}
	}
	
	public static String buildDataSourceKey(String group,String tenantId,boolean master,int index) {
		StringBuilder builder = new StringBuilder(group).append(GlobalConstants.UNDER_LINE);
		if(tenantId != null) {
			builder.append(tenantId).append(GlobalConstants.UNDER_LINE);
		}
		builder.append(master ? MASTER_KEY : SLAVE_KEY).append(GlobalConstants.UNDER_LINE);
		builder.append(index);
		return builder.toString();
	}

	@Override
	public String toString() {
		return "DataSourceConfig [group=" + group + ", tenantRouteKey=" + tenantRouteKey + ", url=" + url + ", username=" + username
				+ ", master=" + master + ", index=" + index + ", maxActive=" + maxActive + "]";
	}
	
}
