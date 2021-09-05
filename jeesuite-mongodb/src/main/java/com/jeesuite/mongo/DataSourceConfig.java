/**
 * Confidential and Proprietary Copyright 2019 By 卓越里程教育科技有限公司 All Rights Reserved
 */
package com.jeesuite.mongo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * 
 * <br>
 * Class Name   : MongoDbConfig
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年5月22日
 */
@JsonInclude(Include.NON_NULL)
public class DataSourceConfig {

	private String tenantId;
	private String dbType;
	private String uri;
	private String servers;
	private String user;
	private String password;
	private String authDatabaseName;
	private String databaseName;
	private String replicaSetName;
	private boolean readOnly;
	
	/**
	 * @return the tenantId
	 */
	public String getTenantId() {
		return tenantId;
	}
	/**
	 * @param tenantId the tenantId to set
	 */
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}
	
	/**
	 * @return the dbType
	 */
	public String getDbType() {
		return dbType;
	}
	/**
	 * @param dbType the dbType to set
	 */
	public void setDbType(String dbType) {
		this.dbType = dbType;
	}
	
	/**
	 * @return the servers
	 */
	public String getServers() {
		return servers;
	}
	/**
	 * @param servers the servers to set
	 */
	public void setServers(String servers) {
		this.servers = servers;
	}
	
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}
	/**
	 * @param user the user to set
	 */
	public void setUser(String user) {
		this.user = user;
	}
	
	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}
	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getAuthDatabaseName() {
		return authDatabaseName;
	}
	public void setAuthDatabaseName(String authDatabaseName) {
		this.authDatabaseName = authDatabaseName;
	}
	/**
	 * @return the databaseName
	 */
	public String getDatabaseName() {
		return databaseName;
	}
	/**
	 * @param databaseName the databaseName to set
	 */
	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}
	
	public String getReplicaSetName() {
		return replicaSetName;
	}
	public void setReplicaSetName(String replicaSetName) {
		this.replicaSetName = replicaSetName;
	}
	/**
	 * @return the readOnly
	 */
	public boolean isReadOnly() {
		return readOnly;
	}
	/**
	 * @param readOnly the readOnly to set
	 */
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}
	@Override
	public String toString() {
		return "DataSourceConfig [tenantId=" + tenantId + ", servers=" + servers + ", user=" + user + ", databaseName="
				+ databaseName + ", replicaSetName=" + replicaSetName + "]";
	}
	
	
	
	
}
