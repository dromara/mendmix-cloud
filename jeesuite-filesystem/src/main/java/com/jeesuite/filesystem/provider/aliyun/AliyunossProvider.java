package com.jeesuite.filesystem.provider.aliyun;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.time.DateUtils;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.CreateBucketRequest;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.jeesuite.filesystem.UploadObject;
import com.jeesuite.filesystem.provider.AbstractProvider;


/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年8月23日
 */
public class AliyunossProvider extends AbstractProvider{

	public static final String NAME = "aliyun";
	
	private OSSClient ossClient;
	private String bucketName;
	private String urlprefix = "/";
	private boolean isPrivate;
	
	public AliyunossProvider(String urlprefix,String endpoint, String bucketName, String accessKey, String secretKey,boolean isPrivate) {
		
		Validate.notBlank(endpoint, "[endpoint] not defined");
		Validate.notBlank(bucketName, "[bucketName] not defined");
		Validate.notBlank(accessKey, "[accessKey] not defined");
		Validate.notBlank(secretKey, "[secretKey] not defined");
		Validate.notBlank(urlprefix, "[urlprefix] not defined");
		
		ossClient = new OSSClient(endpoint, accessKey, secretKey);
		this.bucketName = bucketName;
		this.urlprefix = urlprefix.endsWith("/") ? urlprefix : (urlprefix + "/");
		this.isPrivate = isPrivate;
		if (!ossClient.doesBucketExist(bucketName)) {
			System.out.println("Creating bucket " + bucketName + "\n");
            ossClient.createBucket(bucketName);
            CreateBucketRequest createBucketRequest= new CreateBucketRequest(bucketName);
            createBucketRequest.setCannedACL(isPrivate ? CannedAccessControlList.Private : CannedAccessControlList.PublicRead);
            ossClient.createBucket(createBucketRequest);
		}
	}
	
	@Override
	public String upload(UploadObject object) {
		try {
			PutObjectRequest request = null;
			if(object.getFile() != null){
				request = new PutObjectRequest(bucketName, object.getFileName(), object.getFile());
			}else if(object.getBytes() != null){
				request = new PutObjectRequest(bucketName, object.getFileName(), new ByteArrayInputStream(object.getBytes()));
			}else if(object.getInputStream() != null){
				request = new PutObjectRequest(bucketName, object.getFileName(), object.getInputStream());
			}else{
				throw new IllegalArgumentException("upload object is NULL");
			}
			
			PutObjectResult result = ossClient.putObject(request);
			if(result.getResponse() == null){
				return isPrivate ? object.getFileName() : urlprefix + object.getFileName();
			}
			if(result.getResponse().isSuccessful()){
				return result.getResponse().getUri();
			}else{
				throw new RuntimeException(result.getResponse().getErrorResponseAsString());
			}
		} catch (OSSException e) {
			throw new RuntimeException(e.getErrorMessage());
		}
	}



	@Override
	public String createUploadToken(Map<String, Object> metadata, long expires, String... fileNames) {
		return null;
	}

	@Override
	public boolean delete(String fileName) {
		ossClient.deleteObject(bucketName, fileName);
		return true;
	}
	
	@Override
	public String getDownloadUrl(String file) {
		//ObjectAcl objectAcl = ossClient.getObjectAcl(bucketName, key);
		if(isPrivate){
			URL url = ossClient.generatePresignedUrl(bucketName, file, DateUtils.addHours(new Date(), 1));
			return url.toString();
		}
		return urlprefix + file;
	}


	@Override
	public void close() throws IOException {
		ossClient.shutdown();
	}

	@Override
	public String name() {
		return NAME;
	}
}
