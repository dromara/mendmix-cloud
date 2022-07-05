/*
 * Copyright 2016-2020 www.mendmix.com.
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
package com.mendmix.cos.provider.huawei;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.cos.BucketConfig;
import com.mendmix.cos.CObjectMetadata;
import com.mendmix.cos.CUploadObject;
import com.mendmix.cos.CUploadResult;
import com.mendmix.cos.CosProviderConfig;
import com.mendmix.cos.UploadTokenParam;
import com.mendmix.cos.provider.AbstractProvider;
import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.CreateBucketRequest;
import com.obs.services.model.DeleteObjectRequest;
import com.obs.services.model.DeleteObjectResult;
import com.obs.services.model.GrantAndPermission;
import com.obs.services.model.GroupGrantee;
import com.obs.services.model.HttpMethodEnum;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.ObsBucket;
import com.obs.services.model.ObsObject;
import com.obs.services.model.Permission;
import com.obs.services.model.PutObjectResult;
import com.obs.services.model.TemporarySignatureRequest;
import com.obs.services.model.TemporarySignatureResponse;
import com.qcloud.cos.utils.IOUtils;


public class HuaweicloudProvider extends AbstractProvider {

    public static final String NAME = "huaweicloud";

    private static Logger logger = LoggerFactory.getLogger(HuaweicloudProvider.class);
    private ObsClient obsClient;

    public HuaweicloudProvider(CosProviderConfig conf){
        super(conf);
        if(StringUtils.isBlank(conf.getRegionName())) {
        	conf.setRegionName("cn-south-1");
        }
        String endpoint = conf.getEndpoint();
		if(endpoint == null) {
			endpoint = String.format("https://obs.%s.myhuaweicloud.com", conf.getRegionName());
		}
        ObsConfiguration obsConfiguration = new ObsConfiguration();
        obsConfiguration.setEndPoint(endpoint);
        obsClient = new ObsClient(conf.getAccessKey(), conf.getSecretKey(), obsConfiguration);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean existsBucket(String bucketName) {
        boolean exists = obsClient.headBucket(bucketName);
        return exists;
    }

    @Override
    public void createBucket(String bucketName, boolean isPrivate) {
        if (existsBucket(bucketName)) {
            throw new RuntimeException("bucket[" + bucketName + "] 已经存在");
        }
        CreateBucketRequest request = new CreateBucketRequest(bucketName, conf.getRegionName());
        ObsBucket bucket = new ObsBucket();
        bucket.setBucketName(bucketName);
        AccessControlList acl=null;
        if(isPrivate){
            acl=AccessControlList.REST_CANNED_PRIVATE;
        }else{
            acl=AccessControlList.REST_CANNED_PUBLIC_READ;
        }
        request.setAcl(acl);
        obsClient.createBucket(request);
    }

    @Override
    public void deleteBucket(String bucketName) {
        if (!existsBucket(bucketName)) {
            logger.info("MENDMIX-TRACE-LOGGGING-->> Bucket[{}]不存在", bucketName);
            return ;
        }
        ObjectListing objectListing = obsClient.listObjects(bucketName);
        if (objectListing != null && !objectListing.getObjects().isEmpty()) {
            logger.error("MENDMIX-TRACE-LOGGGING-->> 桶[{}]不为空， 不能删除", bucketName);
            throw new RuntimeException("桶["+bucketName+"]不为空， 不能删除");
        }
        obsClient.deleteBucket(bucketName);
    }

    @Override
    public BucketConfig getBucketConfig(String bucketName) {
        if (!existsBucket(bucketName)) {
            logger.info("MENDMIX-TRACE-LOGGGING-->> Bucket[{}]不存在", bucketName);
            return null;
        }
        boolean isPrivate=false;
        if (isBucketPrivate(bucketName)) {
            isPrivate=true;
        }else{
            isPrivate=false;
        }

        return new BucketConfig(bucketName, isPrivate, null);
    }

    @Override
    public CUploadResult upload(CUploadObject object) {
        String bucketName = object.getBucketName();
        if (StringUtils.isBlank(bucketName)) {
            throw new MendmixBaseException("BucketName 不能为空");
        }
        InputStream inputStream = object.getInputStream();
        File file = object.getFile();
        String fileKey = object.getFileKey();
        byte[] bytes = object.getBytes();
        long size = 0;
        PutObjectResult putObjectResult=null;
        try {
        	ObjectMetadata metadata = new ObjectMetadata();
            if(object.getMimeType() != null) {
            	metadata.setContentType(object.getMimeType());
            }
            if (file != null) {
                putObjectResult = obsClient.putObject(bucketName, fileKey, file, metadata);
                size = file.length();
            } else if (bytes != null) {
                inputStream = new ByteArrayInputStream(bytes);
                putObjectResult = obsClient.putObject(bucketName, fileKey, inputStream,metadata);
                size = bytes.length;
                inputStream.close();
            } else if (inputStream != null) {
                putObjectResult=obsClient.putObject(bucketName, fileKey, inputStream,metadata);
                size=inputStream.available();
            }else{
                throw new MendmixBaseException("upload object is NULL");
            }
            if (putObjectResult != null) {
                AccessControlList acl = new AccessControlList();
                if (!isBucketPrivate(bucketName)) {
                    acl=AccessControlList.REST_CANNED_PUBLIC_READ;
                }
                obsClient.setObjectAcl(bucketName, fileKey, acl);
                CUploadResult uploadResult = new CUploadResult(fileKey, getDownloadUrl(object.getBucketName(),fileKey, 300), null);
                uploadResult.setMimeType(object.getMimeType());
                uploadResult.setFileSize(size);
                return uploadResult;
            }
        } catch (Exception e) {
            logger.error(String.format("MENDMIX-TRACE-LOGGGING-->> 上传文件出错, bucketName:%s, fileKey:%s", bucketName, fileKey), e);
            throw new MendmixBaseException("上传文件错误");
        }
        return null;
    }

    @Override
    public boolean exists(String bucketName, String fileKey) {
        if (!existsBucket(bucketName)) {
            return false;
        }
        ObsObject object = null;
        try {
            object = obsClient.getObject(bucketName, fileKey);
        } catch (Exception e) {}
        return object!=null;
    }

    @Override
    public boolean delete(String bucketName, String fileKey) {
        if (!exists(bucketName, fileKey)) {
            return false;
        }
        DeleteObjectRequest request = new DeleteObjectRequest();
        request.setBucketName(bucketName);
        request.setObjectKey(fileKey);
        DeleteObjectResult result = obsClient.deleteObject(request);
        return result.isDeleteMarker();
    }

    @Override
    public byte[] getObjectBytes(String bucketName, String fileKey) {
        if (!existsBucket(bucketName)) {
            logger.info("MENDMIX-TRACE-LOGGGING-->> Bucket[{}]不存在", bucketName);
            return null;
        }
        try {
            ObsObject object = obsClient.getObject(bucketName, fileKey);
            InputStream inputStream = object.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(inputStream);
            inputStream.close();
            return bytes;
        } catch (Exception e) {
            logger.error("MENDMIX-TRACE-LOGGGING-->> 获取字节, bucketName={}, fileKey={}, e={}", bucketName, fileKey, ExceptionUtils.getMessage(e));
        }
        return null;
    }

    @Override
    public InputStream getObjectInputStream(String bucketName, String fileKey) {
        if (!existsBucket(bucketName)) {
            logger.info("MENDMIX-TRACE-LOGGGING-->> Bucket[{}]不存在", bucketName);
            return null;
        }
        try {
            ObsObject object = obsClient.getObject(bucketName, fileKey);
            InputStream inputStream = object.getObjectContent();
            return inputStream;
        }catch (Exception e){
            logger.error("获取流失败, bucketName={}, fileKey={}, e={}", bucketName, fileKey, ExceptionUtils.getMessage(e));
            throw new MendmixBaseException(e.getMessage());
        }
    }

    @Override
    public Map<String, Object> createUploadToken(UploadTokenParam param) {
        return null;
    }

    @Override
    public CObjectMetadata getObjectMetadata(String bucketName, String fileKey) {
        ObjectMetadata objectMetadata = obsClient.getObjectMetadata(bucketName, fileKey);
        if (objectMetadata == null) {
            return null;
        }
        CObjectMetadata result = new CObjectMetadata();
        Map<String, Object> customMetadata = objectMetadata.getMetadata();
        if (customMetadata != null) {
            Map<String, String> metadata= Maps.newHashMap();
            for (Map.Entry<String, Object> entry : customMetadata.entrySet()) {
                metadata.put(entry.getKey(), entry.getValue().toString());
            }
            result.setCustomMetadatas(metadata);
        }
        result.setMimeType(objectMetadata.getContentType());
        result.setFilesize(objectMetadata.getContentLength());

        return result;
    }

    @Override
    public void close() {
        try {
            if (obsClient!=null) {
                obsClient.close();
            }
        } catch (Exception e) {
            logger.error("MENDMIX-TRACE-LOGGGING-->> obsClient关闭失败, e={}", ExceptionUtils.getMessage(e));
        }
    }

    @Override
    protected String buildBucketUrlPrefix(String bucketName) {
    	//mendmix.obs.cn-south-1.myhuaweicloud.com
    	return String.format("https://%s.obs.%s.myhuaweicloud.com", bucketName,conf.getSecretKey());
    }

    @Override
    protected String generatePresignedUrl(String bucketName, String fileKey, int expireInSeconds) {
        //默认5分钟， 最长7天
        if (!exists(bucketName, fileKey)) {
            throw new MendmixBaseException("对象[bucketName=" + bucketName + ",fileKey=" + fileKey + "]不存在");
        }
        TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.GET, expireInSeconds);
        req.setBucketName(bucketName);
        req.setObjectKey(fileKey);
        TemporarySignatureResponse res = obsClient.createTemporarySignature(req);
        String signedUrl = res.getSignedUrl();
        return signedUrl;
    }

    public boolean isBucketPrivate(String bucketName){
        if (!existsBucket(bucketName)) {
            throw new RuntimeException("bucket["+bucketName+"]不存在");
        }
        AccessControlList acl = obsClient.getBucketAcl(bucketName);
        Set<GrantAndPermission> grants = acl.getGrants();
        if (grants != null) {
            for (GrantAndPermission grant : grants) {
                if (grant.getGrantee().equals(GroupGrantee.ALL_USERS) && grant.getPermission().equals(Permission.PERMISSION_READ)) {
                    return false;
                }
            }
        }
        return true;
    }
}
