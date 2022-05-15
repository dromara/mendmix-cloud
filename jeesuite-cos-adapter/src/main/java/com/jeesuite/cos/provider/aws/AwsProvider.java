/*
 * Copyright 2016-2020 www.jeesuite.com.
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
package com.jeesuite.cos.provider.aws;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.GlobalConstants;
import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.cos.BucketConfig;
import com.jeesuite.cos.CObjectMetadata;
import com.jeesuite.cos.CUploadObject;
import com.jeesuite.cos.CUploadResult;
import com.jeesuite.cos.CosProviderConfig;
import com.jeesuite.cos.UploadTokenParam;
import com.jeesuite.cos.provider.AbstractProvider;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketResponse;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;


public class AwsProvider extends AbstractProvider {

    public static final String NAME = "aws";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsProvider.class);

    private S3Client s3Client = null;
    private S3Presigner s3Presigner = null;
    private Region region = null;

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

    public AwsProvider(CosProviderConfig conf) {
        super(conf);
        String regionName = conf.getRegionName();
        if(StringUtils.isBlank(regionName)) {
            conf.setRegionName("china-south-1");
        }
        region=Region.of(regionName);
        s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(new AwsCredentialsProvider() {
                    @Override
                    public AwsCredentials resolveCredentials() {
                        return new AwsCredentials() {
                            @Override
                            public String accessKeyId() {
                                return conf.getAccessKey();
                            }

                            @Override
                            public String secretAccessKey() {
                                return conf.getSecretKey();
                            }
                        };
                    }
                }).build();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean existsBucket(String bucketName) {
        ListBucketsResponse listBucketsResponse = s3Client.listBuckets(ListBucketsRequest.builder().build());
        List<Bucket> buckets = listBucketsResponse.buckets();
        if (buckets != null) {
            for (Bucket bucket : buckets) {
                if (bucket.name().equals(bucketName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void createBucket(String bucketName, boolean isPrivate) {
        try {
            BucketCannedACL acl=null;
            if (isPrivate) {
                acl=BucketCannedACL.PRIVATE;
            }else{
                acl=BucketCannedACL.PUBLIC_READ;
            }
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName)
                    .acl(acl).build());
            s3Client.putBucketPolicy(PutBucketPolicyRequest.builder().bucket(bucketName).policy(String.format(publicPolicyTemplate, bucketName,bucketName)).build());
        } catch (Exception e) {
            LOGGER.error("创建Bucket[{}]出错, e={}", bucketName, ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public void deleteBucket(String bucketName) {
        try {
            s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
        } catch (Exception e) {
            LOGGER.error("删除Bucket[{}]出错, e={}", bucketName, ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public BucketConfig getBucketConfig(String bucketName) {
        GetBucketPolicyResponse bucketPolicy = s3Client.getBucketPolicy(GetBucketPolicyRequest.builder().bucket(bucketName).build());
        Boolean isPrivate=false;
        isPrivate = StringUtils.isEmpty(bucketPolicy.policy()) ? true : false;
        BucketConfig config=new BucketConfig(bucketName, isPrivate, null);
        return config;
    }

    @Override
    public CUploadResult upload(CUploadObject object) {
        try {
            String bucketName = object.getBucketName();
            if (StringUtils.isEmpty(bucketName)) {
                throw new JeesuiteBaseException("BucketName 不能为空");
            }
            String fileKey = object.getFileKey();
            PutObjectResponse putObjectResponse = null;
            long size=0;
            if (object.getFile() != null) {
                size=object.getFile().length();
                PutObjectRequest putRequest = PutObjectRequest
                        .builder()
                        .bucket(bucketName)
                        .key(fileKey)
                        .contentType(object.getMimeType())
                        .build();
                putObjectResponse = s3Client.putObject(putRequest, object.getFile().toPath());
            } else if (object.getInputStream() != null) {
                size=object.getInputStream().available();
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileKey)
                        .contentType(object.getMimeType())
                        .build();
                putObjectResponse = s3Client.putObject(putRequest, RequestBody.fromInputStream(object.getInputStream(), object.getInputStream().available()));
            } else if (object.getBytes() != null) {
                size=object.getBytes().length;
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileKey)
                        .contentType(object.getMimeType())
                        .build();
                putObjectResponse = s3Client.putObject(putRequest, RequestBody.fromBytes(object.getBytes()));
            }
            if (putObjectResponse != null) {
                CUploadResult uploadResult=new CUploadResult(fileKey, getDownloadUrl(bucketName, fileKey, 300), null);
                uploadResult.setMimeType(object.getMimeType());
                uploadResult.setFileSize(size);
                return uploadResult;
            }
        } catch (JeesuiteBaseException e){
            throw e;
        } catch (Exception e) {
            LOGGER.warn("上传失败, e={}", ExceptionUtils.getMessage(e), e);
            throw new JeesuiteBaseException(e.getMessage());
        }
        return null;
    }

    @Override
    public boolean exists(String bucketName, String fileKey) {
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();
        HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
        if (headObjectResponse != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean delete(String bucketName, String fileKey) {
        try {
            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            DeleteBucketResponse deleteBucketResponse = s3Client.deleteBucket(deleteBucketRequest);
            return deleteBucketResponse.sdkHttpResponse().isSuccessful();
        } catch (Exception e) {
            LOGGER.error("删除Bucket[{}]出错, e={}", bucketName, ExceptionUtils.getMessage(e), e);
        }
        return false;
    }

    @Override
    public byte[] getObjectBytes(String bucketName, String fileKey) {
        byte[] bytes = new byte[0];
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();
            ResponseBytes<GetObjectResponse> objectAsBytes = s3Client.getObjectAsBytes(getObjectRequest);
            bytes = objectAsBytes.asByteArray();
        } catch (Exception e) {
            LOGGER.error("getObjectBytes出错, bucketName={}, fileKey={}, e={}", bucketName, fileKey, ExceptionUtils.getMessage(e), e);
        }
        return bytes;
    }

    @Override
    public InputStream getObjectInputStream(String bucketName, String fileKey) {
        byte[] bytes = getObjectBytes(bucketName, fileKey);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        return inputStream;
    }

    @Override
    public Map<String, Object> createUploadToken(UploadTokenParam param) {
        return null;
    }

    @Override
    public CObjectMetadata getObjectMetadata(String bucketName, String fileKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();
        ResponseInputStream<GetObjectResponse> object = s3Client.getObject(getObjectRequest);
        Map<String, String> metadata = object.response().metadata();
        if (metadata != null) {
            CObjectMetadata cObjectMetadata=new CObjectMetadata();
            cObjectMetadata.setCustomMetadatas(metadata);
            return cObjectMetadata;
        }
        return null;
    }

    @Override
    public void close() {
        s3Client.close();
        s3Presigner.close();
    }

    @Override
    protected String buildBucketUrlPrefix(String bucketName) {
        String baseUrl=conf.getEndpoint();
        if (!baseUrl.endsWith(GlobalConstants.PATH_SEPARATOR)) {
            baseUrl = baseUrl + GlobalConstants.PATH_SEPARATOR;
        }
        String urlPrefix = baseUrl.replace("://", "://" + bucketName+".");
        return urlPrefix;
    }

    @Override
    protected String generatePresignedUrl(String bucketName, String fileKey, int expireInSeconds) {

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();
        GetObjectPresignRequest getObjectPresignRequest =  GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(getObjectRequest)
                .build();
        PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(getObjectPresignRequest);
        String url = presignedGetObjectRequest.url().toString();
        return url;
    }
}
