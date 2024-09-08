/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.cos;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dromara.mendmix.common.util.JsonUtils;

/**
 * 
 * <br>
 * Class Name   : CosProviderConfig
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年1月3日
 */
public class CosProviderConfig {

	private String appId;
	private String accessKey;
	@JsonIgnore
	private String secretKey;
	private String endpoint;
	private String regionName;
	private long maxAllowdSingleFileSize = 500 * 1024L * 1024L;
	private int maxConnectionsCount = 100;
	
	private List<BucketConfig> bucketConfigs;

	
	/**
	 * @return the appId
	 */
	public String getAppId() {
		return appId;
	}
	/**
	 * @param appId the appId to set
	 */
	public void setAppId(String appId) {
		this.appId = appId;
	}
	/**
	 * @return the accessKey
	 */
	public String getAccessKey() {
		return accessKey;
	}
	/**
	 * @param accessKey the accessKey to set
	 */
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}
	/**
	 * @return the secretKey
	 */
	public String getSecretKey() {
		return secretKey;
	}
	/**
	 * @param secretKey the secretKey to set
	 */
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
	
	
	public String getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	/**
	 * @return the regionName
	 */
	public String getRegionName() {
		return regionName;
	}
	/**
	 * @param regionName the regionName to set
	 */
	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}
	
	/**
	 * @return the maxAllowdSingleFileSize
	 */
	public long getMaxAllowdSingleFileSize() {
		return maxAllowdSingleFileSize;
	}
	/**
	 * @param maxAllowdSingleFileSize the maxAllowdSingleFileSize to set
	 */
	public void setMaxAllowdSingleFileSize(long maxAllowdSingleFileSize) {
		this.maxAllowdSingleFileSize = maxAllowdSingleFileSize;
	}
	/**
	 * @return the maxConnectionsCount
	 */
	public int getMaxConnectionsCount() {
		return maxConnectionsCount;
	}
	/**
	 * @param maxConnectionsCount the maxConnectionsCount to set
	 */
	public void setMaxConnectionsCount(int maxConnectionsCount) {
		this.maxConnectionsCount = maxConnectionsCount;
	}
	
	public List<BucketConfig> getBucketConfigs() {
		return bucketConfigs == null ? (bucketConfigs = new ArrayList<>()) : bucketConfigs;
	}
	public void setBucketConfigs(List<BucketConfig> bucketConfigs) {
		this.bucketConfigs = bucketConfigs;
	}
	
	public void addBucketConfig(BucketConfig config) {
		getBucketConfigs().add(config);
	}
	
	@Override
	public String toString() {
		return JsonUtils.toJson(this);
	}
	
	
	
}
