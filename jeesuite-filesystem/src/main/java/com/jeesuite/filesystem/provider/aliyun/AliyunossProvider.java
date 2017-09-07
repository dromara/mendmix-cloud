package com.jeesuite.filesystem.provider.aliyun;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import com.aliyun.oss.OSSClient;
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
	
	public AliyunossProvider(String endpoint, String bucketName, String accessKey, String secretKey) {
		ossClient = new OSSClient(endpoint, accessKey, secretKey);
		this.bucketName = bucketName;
	}
	
	@Override
	public String upload(UploadObject object) {
		PutObjectResult result = null;
		try {
			if(object.getFile() != null){
				result = ossClient.putObject(bucketName, object.getFileName(), object.getFile());
			}else if(object.getBytes() != null){
				result = ossClient.putObject(bucketName, object.getFileName(), new ByteArrayInputStream(object.getBytes()));
			}else if(object.getInputStream() != null){
				result = ossClient.putObject(bucketName, object.getFileName(), object.getInputStream());
			}else{
				throw new IllegalArgumentException("upload object is NULL");
			}
			return checkPutObjectResult(result);
		} catch (Exception e) {
			
		}
		return null;
	}



	@Override
	public String createUploadToken(Map<String, Object> metadata, long expires, String... fileNames) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean delete(String fileName) {
		ossClient.deleteObject(bucketName, fileName);
		return true;
	}
	
	@Override
	public String getDownloadUrl(String file) {
		return null;
	}


	@Override
	public void close() throws IOException {
		ossClient.shutdown();
	}

	@Override
	public String name() {
		return NAME;
	}
	

	private String checkPutObjectResult(PutObjectResult result){
		if(result.getResponse().isSuccessful()){
			return result.getResponse().getUri();
		}else{
			throw new RuntimeException(result.getResponse().getErrorResponseAsString());
		}
	}
}
