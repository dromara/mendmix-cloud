package com.jeesuite.cos.provider.minio;

import java.io.InputStream;
import java.util.Map;

import com.jeesuite.cos.BucketConfig;
import com.jeesuite.cos.CObjectMetadata;
import com.jeesuite.cos.CUploadObject;
import com.jeesuite.cos.CUploadResult;
import com.jeesuite.cos.CosProviderConfig;
import com.jeesuite.cos.UploadTokenParam;
import com.jeesuite.cos.provider.AbstractProvider;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;

public class MinioProvider extends AbstractProvider{

	public static final String NAME = "minio";
	
	private MinioClient minioClient;
	
	public MinioProvider(CosProviderConfig conf) {
		super(conf);
		minioClient = MinioClient.builder()
				                 .endpoint(conf.getEndpoint())//
				                 .credentials(conf.getAccessKey(), conf.getSecretKey())
				                 .build();
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public boolean existsBucket(String bucketName) {
		return false;
	}

	@Override
	public void createBucket(String bucketName, boolean isPrivate) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteBucket(String bucketName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public BucketConfig getBucketConfig(String bucketName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CUploadResult upload(CUploadObject object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exists(String bucketName, String fileKey) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(String bucketName, String fileKey) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public byte[] getObjectBytes(String bucketName, String fileKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getObjectInputStream(String bucketName, String fileKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> createUploadToken(UploadTokenParam param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CObjectMetadata getObjectMetadata(String bucketName, String fileKey) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	protected String buildBucketUrlPrefix(String bucketName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String generatePresignedUrl(String bucketName, String fileKey, int expireInSeconds) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void close() {}

}
