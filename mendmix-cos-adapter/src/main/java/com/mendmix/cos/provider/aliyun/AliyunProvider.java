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
package com.mendmix.cos.provider.aliyun;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.time.DateUtils;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.CreateBucketRequest;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PolicyConditions;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.cos.BucketConfig;
import com.mendmix.cos.CObjectMetadata;
import com.mendmix.cos.CUploadObject;
import com.mendmix.cos.CUploadResult;
import com.mendmix.cos.CosProviderConfig;
import com.mendmix.cos.UploadTokenParam;
import com.mendmix.cos.provider.AbstractProvider;



/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年8月23日
 */
public class AliyunProvider extends AbstractProvider{

	public static final String NAME = "aliyun";
	
	private static final String DEFAULT_CALLBACK_BODY = "filename=${object}&size=${size}&mimeType=${mimeType}&height=${imageInfo.height}&width=${imageInfo.width}";
	
	private OSS ossClient;

	
	public AliyunProvider(CosProviderConfig conf) {
		super(conf);
		Validate.notBlank(conf.getRegionName(),"conf[regionName] is required");
		String endpoint = conf.getEndpoint();
		if(endpoint == null) {
			endpoint = String.format("https://oss-%s.aliyuncs.com", conf.getRegionName());
		}
		ClientBuilderConfiguration buildConf = new ClientBuilderConfiguration();
		// 设置是否支持CNAME。CNAME用于将自定义域名绑定到目标Bucket。
		buildConf.setSupportCname(true);
		ossClient = new OSSClientBuilder().build(endpoint, conf.getAccessKey(), conf.getSecretKey(),buildConf);
	}


	@Override
	public String name() {
		return NAME;
	}

	@Override
	public boolean existsBucket(String bucketName) {
		return ossClient.doesBucketExist(bucketName);
	}

	@Override
	public void createBucket(String bucketName,boolean isPrivate) {
		if(ossClient.doesBucketExist(bucketName)) {
			throw new MendmixBaseException(406, "bucketName["+bucketName+"]已存在");
		}
		CreateBucketRequest request = new CreateBucketRequest(bucketName);
		if(isPrivate) {
			request.setCannedACL(CannedAccessControlList.Private);
		}else {
			request.setCannedACL(CannedAccessControlList.PublicRead);
		}
		ossClient.createBucket(request);
	}

	@Override
	public BucketConfig getBucketConfig(String bucketName) {
		if(!ossClient.doesBucketExist(bucketName))return null;
		CannedAccessControlList acl = ossClient.getBucketAcl(bucketName).getCannedACL();
		return new BucketConfig(bucketName, acl == CannedAccessControlList.Private, null);
	}

	@Override
	public void deleteBucket(String bucketName) {
		ossClient.deleteBucket(bucketName);
	}


	@Override
	public CUploadResult upload(CUploadObject object) {
		try {
			PutObjectRequest request;
			String fileKey = object.getFileKey();
			String bucketName = buildBucketName(object.getBucketName());
			if(object.getFile() != null){
				request = new PutObjectRequest(bucketName, fileKey, object.getFile());
			}else if(object.getBytes() != null){
				ByteArrayInputStream inputStream = new ByteArrayInputStream(object.getBytes());
				ObjectMetadata objectMetadata = new ObjectMetadata();
		        objectMetadata.setContentLength(object.getFileSize());
				request = new PutObjectRequest(bucketName, fileKey, inputStream, objectMetadata);
			}else if(object.getInputStream() != null){
				ObjectMetadata objectMetadata = new ObjectMetadata();
		        objectMetadata.setContentLength(object.getFileSize());
				request = new PutObjectRequest(bucketName, fileKey, object.getInputStream(), objectMetadata);
			}else{
				throw new IllegalArgumentException("upload object is NULL");
			}
			
			PutObjectResult result = ossClient.putObject(request);

			if(result.getResponse() == null || result.getResponse().isSuccessful()){
				return new CUploadResult(fileKey,getFullPath(object.getBucketName(),fileKey), result.getServerCRC().toString());
			}else{
				throw new RuntimeException(result.getResponse().getErrorResponseAsString());
			}
		} catch (OSSException e) {
			throw new RuntimeException(e.getErrorMessage());
		}
	}


	@Override
	public boolean exists(String bucketName, String fileKey) {
		bucketName = buildBucketName(bucketName);
		fileKey = resolveFileKey(bucketName, fileKey);
		return ossClient.doesObjectExist(bucketName, fileKey);	
	}


	@Override
	public boolean delete(String bucketName, String fileKey) {
		bucketName = buildBucketName(bucketName);
		fileKey = resolveFileKey(bucketName, fileKey);
		ossClient.deleteObject(bucketName, fileKey);
		return true;
	}


	@Override
	public byte[] getObjectBytes(String bucketName, String fileKey) {
		try {
			InputStream inputStream = getObjectInputStream(bucketName, fileKey);
			return IOUtils.toByteArray(inputStream);
		} catch (IOException e) {
			throw new MendmixBaseException(e.getMessage());
		}
	}


	@Override
	public InputStream getObjectInputStream(String bucketName, String fileKey) {
		bucketName = buildBucketName(bucketName);
		fileKey = resolveFileKey(bucketName, fileKey);
		return ossClient.getObject(bucketName, fileKey).getObjectContent();
	}


	@Override
	public Map<String, Object> createUploadToken(UploadTokenParam param) {
		
		BucketConfig config = currentBucketConfig(param.getBucketName());
		
		Map<String, Object> result = new HashMap<>();
		
		PolicyConditions policyConds = new PolicyConditions();
		if(param.getFsizeMin() != null && param.getFsizeMax() != null){			
			policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, param.getFsizeMin(), param.getFsizeMax());
		}else{
			policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 1048576000);
		}
		if(param.getUploadDir() != null){			
			policyConds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, param.getUploadDir());
		}
        
		if(StringUtils.isBlank(param.getCallbackHost())){
			param.setCallbackHost(StringUtils.remove(config.getUrlPrefix(),"/").split(":")[1]);
		}
		
		if(StringUtils.isBlank(param.getCallbackBody())){
			param.setCallbackBody(DEFAULT_CALLBACK_BODY);
		}
		
		Date expire = DateUtils.addSeconds(new Date(), (int)param.getExpires());
		String policy = ossClient.generatePostPolicy(expire, policyConds);
        String policyBase64 = null;
        String callbackBase64 = null;
        try {
        	policyBase64 = BinaryUtil.toBase64String(policy.getBytes(StandardCharsets.UTF_8.name()));
        	String callbackJson = param.getCallbackRuleAsJson();
        	if(callbackJson != null){
        		callbackBase64 = BinaryUtil.toBase64String(callbackJson.getBytes(StandardCharsets.UTF_8.name()));
        	}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		String signature = ossClient.calculatePostSignature(policy);
		
		result.put("OSSAccessKeyId", conf.getAccessKey());
		result.put("policy", policyBase64);
		result.put("signature", signature);
		result.put("host", config.getUrlPrefix());
		result.put("dir", param.getUploadDir());
		result.put("expire", String.valueOf(expire.getTime()));
		if(callbackBase64 != null){
			result.put("callback", callbackBase64);
		}
		return result;
	}


	@Override
	public CObjectMetadata getObjectMetadata(String bucketName, String fileKey) {
		try {
			bucketName = buildBucketName(bucketName);
			fileKey = resolveFileKey(bucketName, fileKey);
			ObjectMetadata metadata = ossClient.getObjectMetadata(bucketName, fileKey);
			CObjectMetadata objectMetadata = new CObjectMetadata();
			objectMetadata.setCreateTime(metadata.getLastModified());
			objectMetadata.setMimeType(metadata.getContentType());
			objectMetadata.setFilesize(metadata.getContentLength());
			objectMetadata.setHash(metadata.getContentMD5());
			objectMetadata.setExpirationTime(metadata.getExpirationTime());
			objectMetadata.setCustomMetadatas(metadata.getUserMetadata());
			return objectMetadata;
		} catch (Exception e) {
			throw new MendmixBaseException(500, e.getMessage());
		}
	}


	@Override
	protected String buildBucketUrlPrefix(String bucketName) {
		return String.format("https://%s.oss-%s.aliyuncs.com", bucketName,conf.getRegionName());
	}


	@Override
	protected String generatePresignedUrl(String bucketName, String fileKey, int expireInSeconds) {
		bucketName = buildBucketName(bucketName);
		fileKey = resolveFileKey(bucketName, fileKey);
		URL presignedUrl = ossClient.generatePresignedUrl(bucketName, fileKey, DateUtils.addSeconds(new Date(), expireInSeconds));
		return presignedUrl.toString();
	}

	@Override
	public void close() {
		ossClient.shutdown();
	}

	
}
