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
package org.dromara.mendmix.cos.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import org.dromara.mendmix.common.util.HttpUtils;
import org.dromara.mendmix.cos.BucketConfig;
import org.dromara.mendmix.cos.CosProvider;
import org.dromara.mendmix.cos.CosProviderConfig;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月7日
 */
public abstract class AbstractProvider implements CosProvider{

	protected static final String URL_PREFIX_PATTERN = "(http).*\\.(com|cn)\\/";
	protected static final String HTTP_PREFIX = "http://";
	protected static final String HTTPS_PREFIX = "https://";
	protected static final String DIR_SPLITER = "/";
	private Map<String, BucketConfig> bucketMappings = new HashMap<>();
	
	
	protected CosProviderConfig conf;
	
	
	public AbstractProvider(CosProviderConfig conf) {
		Validate.notBlank(conf.getAccessKey(), "[accessKey] not defined");
		Validate.notBlank(conf.getSecretKey(), "[secretKey] not defined");
		this.conf = conf;
		
		List<BucketConfig> bucketConfigs = conf.getBucketConfigs();
		for (BucketConfig bucketConfig : bucketConfigs) {
			addBucketConfig(bucketConfig);
		}
	}


	protected String getFullPath(String bucketName,String file) {
		if(file.startsWith(HTTP_PREFIX) || file.startsWith(HTTPS_PREFIX)){
			return file;
		}
		return getBucketUrlPrefix(bucketName) + file;
	}
	
	protected String resolveFileKey(String bucketName,String fileUrl) {
		if(!fileUrl.startsWith(HTTP_PREFIX) && !fileUrl.startsWith(HTTPS_PREFIX)){
			return fileUrl;
		}
		String urlprefix = getBucketUrlPrefix(bucketName);
		return fileUrl.replace(urlprefix, StringUtils.EMPTY);
	}

	@Override
	public String downloadAndSaveAs(String bucketName,String file, String localSaveDir) {
		return HttpUtils.downloadFile(getDownloadUrl(bucketName,file,300), localSaveDir);
	}
	
	@Override
	public String getDownloadUrl(String bucketName,String fileKey, int expireInSeconds) {
		BucketConfig config = currentBucketConfig(bucketName);
		String url;
		if(config.isAuth()){
			fileKey = resolveFileKey(bucketName, fileKey);
			String presignedUrl = generatePresignedUrl(bucketName,fileKey,expireInSeconds);
			String urlprefix = getBucketUrlPrefix(bucketName);
			url = presignedUrl.replaceFirst(URL_PREFIX_PATTERN, urlprefix);
		}else{
			url = getFullPath(bucketName,fileKey);
		}
		return url;
	}


	/**
	 * @param bucketName
	 * @return
	 */
	protected String getBucketUrlPrefix(String bucketName) {
		BucketConfig config = currentBucketConfig(bucketName);
		if(StringUtils.isNotBlank(config.getUrlPrefix())) {
			return config.getUrlPrefix();
		}
		synchronized (bucketMappings) {
			if(StringUtils.isNotBlank(config.getUrlPrefix())) {
				return config.getUrlPrefix();
			}
			String urlPrefix = buildBucketUrlPrefix(bucketName);
			if(!urlPrefix.endsWith("/")) {
				urlPrefix = urlPrefix.concat("/");
			}
			config.setUrlPrefix(urlPrefix);
			return urlPrefix;
		}
	}
	
	protected String buildBucketName(String bucketName) {
		if(StringUtils.isBlank(bucketName)){
			throw new IllegalArgumentException("[bucketName] not defined");
		}
		return bucketName;
	}
	
	public BucketConfig currentBucketConfig(String bucketName) {
		BucketConfig config = bucketMappings.get(buildBucketName(bucketName));
		if(config == null) {
			synchronized (bucketMappings) {
				config = getBucketConfig(bucketName);
				if(config != null) {
					addBucketConfig(config);
				}
			}
		}
		if(config == null) {
			throw new IllegalArgumentException("[bucketName] not exists");
		}
		return config;
	}
	
	public void addBucketConfig(BucketConfig bucketConfig) {
		if(bucketConfig == null || bucketConfig.getName() == null)return;
		if(StringUtils.isNotBlank(bucketConfig.getUrlPrefix()) && bucketConfig.getUrlPrefix().endsWith("/")) {
			bucketConfig.setUrlPrefix(bucketConfig.getUrlPrefix().substring(0,bucketConfig.getUrlPrefix().length() - 1));
		}
		bucketConfig.setName(buildBucketName(bucketConfig.getName()));
		bucketMappings.put(bucketConfig.getName(), bucketConfig);
	}


	protected abstract String buildBucketUrlPrefix(String bucketName);
	protected abstract String generatePresignedUrl(String bucketName,String fileKey, int expireInSeconds);

	
}
