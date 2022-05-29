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
package com.mendmix.cos.provider.qcloud;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.time.DateUtils;

import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.async.StandardThreadExecutor;
import com.mendmix.common.async.StandardThreadExecutor.StandardThreadFactory;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.CreateBucketRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.Upload;
import com.tencent.cloud.CosStsClient;
import com.mendmix.cos.BucketConfig;
import com.mendmix.cos.CObjectMetadata;
import com.mendmix.cos.CUploadObject;
import com.mendmix.cos.CUploadResult;
import com.mendmix.cos.CosProviderConfig;
import com.mendmix.cos.FilePathHelper;
import com.mendmix.cos.UploadTokenParam;
import com.mendmix.cos.provider.AbstractProvider;

/**
 * 
 * <br>
 * Class Name   : QcloudProvider
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年1月2日
 */
public class QcloudProvider extends AbstractProvider {

	public static final String NAME = "qcloud";
	private static Pattern bucketNamePattern = Pattern.compile("\\w+\\-[0-9]{5,}");
	
	private COSClient cosclient;
	private TransferManager transferManager;
	private StandardThreadExecutor transferExecutor;
	
	//private Pattern bucketWithAppId = Pattern.compile(".*-[0-9]{3,}$");
	
	/**
	 * @param conf
	 */
	public QcloudProvider(CosProviderConfig conf) {
		super(conf);
		Validate.notBlank(conf.getAppId(), "[appId] not defined");
		//设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
		if(StringUtils.isBlank(conf.getRegionName())){
			conf.setRegionName("ap-guangzhou");
		}

		COSCredentials cred = new BasicCOSCredentials(conf.getAccessKey(), conf.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(conf.getRegionName()));
        clientConfig.setMaxConnectionsCount(conf.getMaxConnectionsCount());
        //生成cos客户端
        cosclient = new COSClient(cred, clientConfig);
        //
        transferExecutor = new StandardThreadExecutor(1, 5,0, TimeUnit.SECONDS, 1,new StandardThreadFactory("cos-transfer-executor"));
        transferManager = new TransferManager(cosclient, transferExecutor);
	}

	@Override
	public String name() {
		return NAME;
	}
	
	@Override
	public boolean existsBucket(String bucketName) {
		bucketName = buildBucketName(bucketName);
		return cosclient.doesBucketExist(bucketName);
	}
	
	@Override
	public BucketConfig getBucketConfig(String bucketName) {
		bucketName = buildBucketName(bucketName);
		if(!cosclient.doesBucketExist(bucketName))return null;
		CannedAccessControlList acl = cosclient.getBucketAcl(bucketName).getCannedAccessControl();
		return new BucketConfig(bucketName, acl == CannedAccessControlList.Private, null);
	}

	@Override
	public void createBucket(String bucketName,boolean isPrivate) {
		bucketName = buildBucketName(bucketName);
		if(cosclient.doesBucketExist(bucketName)){
			throw new MendmixBaseException(406, "bucketName["+bucketName+"]已存在");
		}
		CreateBucketRequest request = new CreateBucketRequest(bucketName);
		if(isPrivate) {
			request.setCannedAcl(CannedAccessControlList.Private);
		}else {
			request.setCannedAcl(CannedAccessControlList.PublicRead);
		}
		cosclient.createBucket(request);
	}

	@Override
	public void deleteBucket(String bucketName) {
		bucketName = buildBucketName(bucketName);
		cosclient.deleteBucket(bucketName);
	}
	
	@Override
	public boolean exists(String bucketName,String fileKey) {
		fileKey = resolveFileKey(bucketName, fileKey);
		bucketName = buildBucketName(bucketName);
		return cosclient.doesObjectExist(bucketName, fileKey);
	}
	
	@Override
	public CUploadResult upload(CUploadObject object) {
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
		
		try {
			if(object.getFileSize() > conf.getMaxAllowdSingleFileSize()){
				Upload upload = transferManager.upload(request);
				com.qcloud.cos.model.UploadResult result = upload.waitForUploadResult();
				return new CUploadResult(fileKey, getFullPath(object.getBucketName(),fileKey), result.getCrc64Ecma()); 
			}else{
				PutObjectResult result = cosclient.putObject(request);
				return new CUploadResult(fileKey,getFullPath(object.getBucketName(),fileKey), result.getContentMd5());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new MendmixBaseException(500, buildMessage(bucketName,e));
		}
	}

	@Override
	protected String generatePresignedUrl(String bucketName,String fileKey, int expireInSeconds) {
		bucketName = buildBucketName(bucketName);
		try {
			URL url = cosclient.generatePresignedUrl(bucketName, fileKey, DateUtils.addSeconds(new Date(), expireInSeconds));
			return url.toString();
		} catch (Exception e) {
			throw new MendmixBaseException(500, buildMessage(bucketName,e));
		}
	}

	@Override
	public boolean delete(String bucketName,String fileKey) {
		try {
			bucketName = buildBucketName(bucketName);
			cosclient.deleteObject(bucketName, fileKey);
		} catch (Exception e) {
			throw new MendmixBaseException(500, buildMessage(bucketName,e));
		} 
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
		try {
			String _bucketName = buildBucketName(bucketName);
			String _fileKey = resolveFileKey(bucketName, fileKey);
			COSObject cosObject = cosclient.getObject(_bucketName, _fileKey);
			return cosObject.getObjectContent();
		} catch (Exception e) {
			throw new MendmixBaseException(500, buildMessage(bucketName,e));
		}
	}

	@Override
	public CObjectMetadata getObjectMetadata(String bucketName, String fileKey) {
		try {
			String _bucketName = buildBucketName(bucketName);
			String _fileKey = resolveFileKey(bucketName, fileKey);
			ObjectMetadata metadata = cosclient.getObjectMetadata(_bucketName, _fileKey);
			CObjectMetadata objectMetadata = new CObjectMetadata();
			objectMetadata.setCreateTime(metadata.getLastModified());
			objectMetadata.setMimeType(metadata.getContentType());
			objectMetadata.setFilesize(metadata.getContentLength());
			objectMetadata.setHash(metadata.getContentMD5());
			objectMetadata.setExpirationTime(metadata.getExpirationTime());
			objectMetadata.setCustomMetadatas(metadata.getUserMetadata());
			return objectMetadata;
		} catch (Exception e) {
			throw new MendmixBaseException(500, buildMessage(bucketName,e));
		}
	}

	//https://github.com/tencentyun/qcloud-cos-sts-sdk/tree/master/java
	@Override
	public Map<String, Object> createUploadToken(UploadTokenParam param) {
		TreeMap<String, Object> config = new TreeMap<String, Object>();
		config.put("SecretId", conf.getAccessKey());
		config.put("SecretKey", conf.getSecretKey());
		config.put("durationSeconds", param.getExpires());
		config.put("bucket", buildBucketName(param.getBucketName()));
		config.put("region", conf.getRegionName());
		//config.put("allowPrefix", "a.jpg");

		// 密钥的权限列表。简单上传、表单上传和分片上传需要以下的权限，其他权限列表请看
		// https://cloud.tencent.com/document/product/436/31923
		String[] allowActions = new String[] {
				// 简单上传
				"name/cos:PutObject",
				// 表单上传、小程序上传
				"name/cos:PostObject",
				// 分片上传
				"name/cos:InitiateMultipartUpload", "name/cos:ListMultipartUploads", "name/cos:ListParts",
				"name/cos:UploadPart", "name/cos:CompleteMultipartUpload" };
		config.put("allowActions", allowActions);

		try {
			org.json.JSONObject json = CosStsClient.getCredential(config);
			return json.toMap();
		} catch (IOException e) {
			throw new MendmixBaseException("生成临时凭证错误:"+e.getMessage());
		}
	}


	@Override
	public void close() {
		cosclient.shutdown();
		transferExecutor.shutdown();
	}
	
	protected String buildBucketName(String bucketName){
		bucketName = super.buildBucketName(bucketName);
		if(bucketName.endsWith(conf.getAppId())) {
			return bucketName;
		}
		if(bucketName.contains(FilePathHelper.MID_LINE) && bucketNamePattern.matcher(bucketName).matches()) {
			return bucketName;
		}
		return new StringBuilder(bucketName).append(FilePathHelper.MID_LINE).append(conf.getAppId()).toString();
	}

	@Override
	protected String buildBucketUrlPrefix(String bucketName) {
		//http://qietitoolstest-1252877917.cos.ap-guangzhou.myqcloud.com/
		StringBuilder urlBuilder = new StringBuilder()
				   .append("http://") //
				   .append(buildBucketName(bucketName)) //
				   .append(".cos.") //
				   .append(conf.getRegionName()) //
				   .append(".myqcloud.com");
		return urlBuilder.toString();
	}
	
	private static String buildMessage(String bucketName,Exception e){
		if(e instanceof CosServiceException){
			if("NoSuchBucket".equals(((CosServiceException)e).getErrorCode())){
				throw new MendmixBaseException(404, "bucketName["+bucketName+"]不存在"); 
			}else if("AccessDenied".equals(((CosServiceException)e).getErrorCode())){
				throw new MendmixBaseException(403, "appId与bucketName["+bucketName+"]不匹配"); 
			}else if("InvalidAccessKeyId".equals(((CosServiceException)e).getErrorCode())){
				throw new MendmixBaseException(40, "AccessKey配置错误"); 
			}
			return ((CosServiceException)e).getErrorMessage();
		}else{
			return e.getMessage();
		}
	}

}
