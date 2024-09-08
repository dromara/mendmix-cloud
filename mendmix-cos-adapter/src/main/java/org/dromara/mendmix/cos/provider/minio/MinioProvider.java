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
package org.dromara.mendmix.cos.provider.minio;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Maps;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.cos.BucketConfig;
import org.dromara.mendmix.cos.CObjectMetadata;
import org.dromara.mendmix.cos.CUploadObject;
import org.dromara.mendmix.cos.CUploadResult;
import org.dromara.mendmix.cos.CosProviderConfig;
import org.dromara.mendmix.cos.UploadTokenParam;
import org.dromara.mendmix.cos.provider.AbstractProvider;

import io.minio.BucketExistsArgs;
import io.minio.GetBucketPolicyArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.UploadObjectArgs;
import io.minio.http.Method;

public class MinioProvider extends AbstractProvider{

	public static final String NAME = "minio";
	
	private final String publicPolicyTemplate ="{\n" +
            "    \"Version\": \"2012-10-17\",\n" +
            "    \"Statement\": [\n" +
            "        {\n" +
            "            \"Effect\": \"Allow\",\n" +
            "            \"Principal\": {\n" +
            "                \"AWS\": [\n" +
            "                    \"*\"\n" +
            "                ]\n" +
            "            },\n" +
            "            \"Action\": [\n" +
            "                \"s3:GetBucketLocation\",\n" +
            "                \"s3:ListBucket\",\n" +
            "                \"s3:ListBucketMultipartUploads\"\n" +
            "            ],\n" +
            "            \"Resource\": [\n" +
            "                \"arn:aws:s3:::%s\"\n" +
            "            ]\n" +
            "        },\n" +
            "        {\n" +
            "            \"Effect\": \"Allow\",\n" +
            "            \"Principal\": {\n" +
            "                \"AWS\": [\n" +
            "                    \"*\"\n" +
            "                ]\n" +
            "            },\n" +
            "            \"Action\": [\n" +
            "                \"s3:ListMultipartUploadParts\",\n" +
            "                \"s3:PutObject\",\n" +
            "                \"s3:AbortMultipartUpload\",\n" +
            "                \"s3:DeleteObject\",\n" +
            "                \"s3:GetObject\"\n" +
            "            ],\n" +
            "            \"Resource\": [\n" +
            "                \"arn:aws:s3:::%s/*\"\n" +
            "            ]\n" +
            "        }\n" +
            "    ]\n" +
            "}";

	
	private MinioClient minioClient;
	
	public MinioProvider(CosProviderConfig conf) {
        super(conf);
        if(StringUtils.isBlank(conf.getRegionName())) {
        	conf.setRegionName("china-south-1");
        }
        String endpoint=conf.getEndpoint();
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .region(conf.getRegionName())
                .credentials(conf.getAccessKey(), conf.getSecretKey())
                .build();
    }

	@Override
	public String name() {
		return NAME;
	}

	@Override
    public boolean existsBucket(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
    }

    /**
     * @param bucketName 只能是数字， 字母， 点（.)和中画线(-)
     * @param isPrivate
     */
    @Override
    public void createBucket(String bucketName, boolean isPrivate) {
        try {
            boolean found = existsBucket(bucketName);
            if (found) {
                throw new MendmixBaseException(406, "bucketName[" + bucketName + "]已存在");
            }
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
          //默认是none， 即私有
            if (!isPrivate) {
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(String.format(publicPolicyTemplate, bucketName, bucketName))
                        .build());
            }

        } catch (MendmixBaseException jbex) {
            throw jbex;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void deleteBucket(String bucketName) {
        try {
            boolean found = existsBucket(bucketName);
            if (!found) {
                return;
            }
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
    }
    
    @Override
    public CUploadResult upload(CUploadObject object) {
        try {
            String bucketName = object.getBucketName();
            if (StringUtils.isEmpty(bucketName)) {
                throw new MendmixBaseException("BucketName 不能为空");
            }
            String fileKey = object.getFileKey();
            InputStream inputStream = object.getInputStream();
            byte[] objectBytes = object.getBytes();
            ObjectWriteResponse objectWriteResponse = null;
            long size=0;
            if (object.getFile() != null) {
                objectWriteResponse = minioClient.uploadObject(UploadObjectArgs.builder()
                        .bucket(bucketName)
                        .filename(object.getFile().getAbsolutePath())
                        .object(fileKey)
                        .contentType(object.getMimeType())
                        .build());
                size=object.getFile().length();
            } else if (objectBytes != null) {
                byte[] bytes = objectBytes;
                ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                objectWriteResponse = minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileKey)
                        .contentType(object.getMimeType())
                        .stream(bis, bytes.length, -1)
                        .build());
                size=bytes.length;
                bis.close();
            } else if (inputStream != null) {
            	 objectWriteResponse = minioClient.putObject(PutObjectArgs.builder()
                         .bucket(bucketName)
                         .object(fileKey)
                         .contentType(object.getMimeType())
                         .stream(inputStream, inputStream.available(), -1)
                         .build());
                 size=inputStream.available();
             } else {
                 throw new MendmixBaseException("upload object is NULL");
             }
             if (objectWriteResponse != null) {
                 CUploadResult uploadResult = new CUploadResult(fileKey, getDownloadUrl(object.getBucketName(),fileKey, 300), null);
                 uploadResult.setMimeType(object.getMimeType());
                 uploadResult.setFileSize(size);
                 return uploadResult;
             }
        } catch (MendmixBaseException jbex) {
            throw jbex;
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public boolean exists(String bucketName, String fileKey) {
        try {
            if (!existsBucket(bucketName)) {
                return false;
            }
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileKey)
                    .build());
            if (stat != null) {
                return true;
            }
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
        return false;
    }
    
    @Override
    public boolean delete(String bucketName, String fileKey) {
        if (!exists(bucketName, fileKey)) {
            return false;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileKey)
                    .build());
            return true;
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
    }
    
    @Override
    protected String buildBucketUrlPrefix(String bucketName) {
        String baseUrl=conf.getEndpoint();
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        return baseUrl + bucketName + "/";
    }

    @Override
    protected String generatePresignedUrl(String bucketName, String fileKey, int expireInSeconds) {
        if (!exists(bucketName, fileKey)) {
            throw new MendmixBaseException("bucket["+bucketName+"] fileKey["+fileKey+"] not exists");
        }
        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(fileKey)
                    .expiry(expireInSeconds, TimeUnit.SECONDS)
                    .build());
            return url;
        }  catch (Exception e) {
        	throw new RuntimeException(e);
        }
    }
    
    @Override
    public BucketConfig getBucketConfig(String bucketName) {
        if (!existsBucket(bucketName)) {
            return null;
        }
        boolean isPrivate=false;
        try {
            String bucketPolicy = minioClient.getBucketPolicy(GetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .build());
            isPrivate = StringUtils.isEmpty(bucketPolicy) ? true : false;
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }


        BucketConfig config = new BucketConfig(bucketName, isPrivate, null);
        return config;
    }
    
    @Override
    public byte[] getObjectBytes(String bucketName, String fileKey) {
        try {
            GetObjectResponse is = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileKey)
                    .build());
            byte[] bytes = IOUtils.toByteArray(is);
            is.close();
            return bytes;
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
    }
    
    @Override
    public InputStream getObjectInputStream(String bucketName, String fileKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileKey)
                    .build());
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> createUploadToken(UploadTokenParam param) {
        //TODO 怎么生成token
        Map<String, Object> result= Maps.newHashMap();
        return result;
    }
    
    @Override
    public CObjectMetadata getObjectMetadata(String bucketName, String fileKey) {
        CObjectMetadata metadata = null;
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileKey)
                    .build());
            metadata = new CObjectMetadata();
            if (stat != null) {
                metadata.setCustomMetadatas(stat.userMetadata());
                metadata.setFilesize(stat.size());
            }
            return metadata;
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
    }
            
	
	@Override
	public void close() {}

}
