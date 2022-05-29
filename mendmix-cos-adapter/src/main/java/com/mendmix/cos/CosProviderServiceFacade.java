/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.cos;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.async.StandardThreadExecutor.StandardThreadFactory;
import com.mendmix.common.http.HttpResponseEntity;
import com.mendmix.common.util.HttpUtils;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.cos.provider.AbstractProvider;
import com.mendmix.cos.provider.aliyun.AliyunProvider;
import com.mendmix.cos.provider.qcloud.QcloudProvider;
import com.mendmix.cos.provider.qiniu.QiniuProvider;

/**
 * 统一服务门面
 * 
 * <br>
 * Class Name   : CosProviderServiceFacade
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2017年6月14日
 */
public class CosProviderServiceFacade implements InitializingBean,DisposableBean {

	private  static final Logger logger = LoggerFactory.getLogger("com.mendmix.cos");
	
	private String type;
	private CosProvider provider;
	private CosProviderConfig config;
	
	private String defaultBucket;
	
	private ThreadPoolExecutor logHandleExecutor;
	
	private String logUrl;
	

	public CosProvider getProvider() {
		return provider;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public void setDefaultBucket(String defaultBucket) {
		this.defaultBucket = defaultBucket;
	}

	public void setConfig(CosProviderConfig config) {
		this.config = config;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		//
		if(defaultBucket == null) {
			defaultBucket = ResourceUtils.getProperty("cos.defaultBucket");
		}
		
		if(type == null) {
			type = ResourceUtils.getAndValidateProperty("cos.provider");	
		}
		if(config == null) {
			config = new CosProviderConfig();
			config.setAccessKey(ResourceUtils.getProperty("cos.accessKey"));
			config.setSecretKey(ResourceUtils.getProperty("cos.secretKey"));
			config.setAppId(ResourceUtils.getProperty("cos.appId"));
			config.setRegionName(ResourceUtils.getProperty("cos.regionName"));
			config.setMaxConnectionsCount(ResourceUtils.getInt("cos.maxConnections", 200));
		}
		
		if(AliyunProvider.NAME.equals(type)) {
			provider = new AliyunProvider(config);
		}else if(QcloudProvider.NAME.equals(type)) {
			provider = new QcloudProvider(config);
		}else if(QiniuProvider.NAME.equals(type)) {
			provider = new QiniuProvider(config);
		}else {
			throw new MendmixBaseException("cos["+type+"] not support");
		}
		
		if(defaultBucket != null) {
			BucketConfig bucketConfig = provider.getBucketConfig(defaultBucket);
			bucketConfig.setUrlPrefix(ResourceUtils.getProperty("cos.defaultUrlPrefix"));
			((AbstractProvider)provider).addBucketConfig(bucketConfig);
		}else {
			Map<String, String> urlPrefixMappings = ResourceUtils.getMappingValues("cos.bucket.urlPrefix.mapping");
			if(urlPrefixMappings != null) {
				urlPrefixMappings.forEach( (bucket,urlPrefix) -> {
					BucketConfig bucketConfig = provider.getBucketConfig(defaultBucket);
					bucketConfig.setUrlPrefix(ResourceUtils.getProperty("cos.defaultUrlPrefix"));
					((AbstractProvider)provider).addBucketConfig(bucketConfig);
				});				
			}
		}
		
		logUrl = ResourceUtils.getProperty("cos.loghandler.url");
		if(logUrl != null && Boolean.parseBoolean(ResourceUtils.getProperty("cos.loghandler.enabled", "true"))) {
			int nThread = ResourceUtils.getInt("cos.loghandler.threads", 1);
			int capacity = ResourceUtils.getInt("cos.loghandler.queueSize", 1000);
			logHandleExecutor = new ThreadPoolExecutor(nThread, nThread,
	                0L, TimeUnit.MILLISECONDS,
	                new LinkedBlockingQueue<Runnable>(capacity),
	                new StandardThreadFactory("cosLogHandleExecutor"));
			logger.info("init logHandleExecutor OK ,nThread:{},queue:{}",nThread,capacity);
		}
	}
	
	@Override
	public void destroy() throws Exception {
		if(provider != null) {
			provider.close();
		}
		if(logHandleExecutor != null) {
			logHandleExecutor.shutdown();
		}
	}
	
	public CUploadResult upload(CUploadObject object) {
		CUploadResult result = null;
		try {
			if(object.getBucketName() == null) {
				object.bucketName(defaultBucket);
			}
			result = provider.upload(object);
			result.setMimeType(object.getMimeType());
			return result;
		} finally {
			if(result != null) {
				syncUploadLog(object,result);
			}
		}
		
	}


	public String getDownloadUrl(String fileKey, int expireInSeconds) {
		return getDownloadUrl(defaultBucket, fileKey, expireInSeconds);
	}
	
	public String getDownloadUrl(String bucketName, String fileKey, int expireInSeconds) {
		return provider.getDownloadUrl(bucketName, fileKey, expireInSeconds);
	}
	
	public boolean exists(String fileKey) {
		return exists(defaultBucket, fileKey);
	}

	public boolean exists(String bucketName, String fileKey) {
		return provider.exists(bucketName, fileKey);
	}
	
	public boolean delete(String fileKey) {
		return delete(defaultBucket, fileKey);
	}

	public boolean delete(String bucketName, String fileKey) {
		return provider.delete(bucketName, fileKey);
	}

	public byte[] getObjectBytes(String bucketName, String fileKey) {
		return provider.getObjectBytes(bucketName, fileKey);
	}

	public InputStream getObjectInputStream(String bucketName, String fileKey) {
		return provider.getObjectInputStream(bucketName, fileKey);
	}

	public String downloadAndSaveAs(String bucketName, String fileKey, String localSaveDir) {
		return provider.downloadAndSaveAs(bucketName, fileKey, localSaveDir);
	}

	public Map<String, Object> createUploadToken(UploadTokenParam param) {
		return provider.createUploadToken(param);
	}

	public CObjectMetadata getObjectMetadata(String bucketName, String fileKey) {
		return provider.getObjectMetadata(bucketName, fileKey);
	}
	
	/**
	 * 上传同步结果
	 * @param result
	 */
	private void syncUploadLog(CUploadObject object,CUploadResult result) {
		if(logHandleExecutor == null || result == null)return;
	    try {
	    	BucketConfig bucketConfig = ((AbstractProvider)provider).currentBucketConfig(object.getBucketName());
	    	UploadResultParam param = new UploadResultParam();
	    	param.setBucketName(bucketConfig.getName());
			if(bucketConfig != null)result.setAuth(bucketConfig.isAuth());
			param.setFileKey(result.getFileKey());
			param.setMimeType(object.getMimeType());
			param.setFileSize(result.getFileSize());
			param.setFileHash(result.getFileHash());
			logHandleExecutor.execute(new Runnable() {
				@Override
				public void run() {
					HttpResponseEntity entity = HttpUtils.postJson(logUrl, JsonUtils.toJson(param));
					if(!entity.isSuccessed()) {
						logger.warn("syncUploadLogError==>{}",entity.getBody());
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
