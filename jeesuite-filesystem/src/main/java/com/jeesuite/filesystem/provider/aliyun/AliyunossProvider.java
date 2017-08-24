package com.jeesuite.filesystem.provider.aliyun;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.PutObjectResult;
import com.jeesuite.filesystem.FileType;
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
	public String upload(String catalog, String fileName, File file) {
		String fileKey = rawFileName(catalog, fileName, null);
		PutObjectResult result = ossClient.putObject(bucketName, fileKey, file);
		return checkPutObjectResult(result);
	}

	@Override
	public String upload(String catalog, String fileName, byte[] data, FileType fileType) {
		String fileKey = rawFileName(catalog, fileName, fileType);
		PutObjectResult result = ossClient.putObject(bucketName, fileKey, new ByteArrayInputStream(data));
		return checkPutObjectResult(result);
	}

	@Override
	public String upload(String catalog, String fileName, InputStream in, FileType fileType) {
		String fileKey = rawFileName(catalog, fileName, fileType);
		PutObjectResult result = ossClient.putObject(bucketName, fileKey, in);
		return checkPutObjectResult(result);
	}

	@Override
	public String upload(String catalog, String fileName, String origUrl) {
		
		return null;
	}

	@Override
	public boolean delete(String fileName) {
		ossClient.deleteObject(bucketName, fileName);
		return true;
	}

	@Override
	public String createUploadToken(String... fileNames) {
		// TODO Auto-generated method stub
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
	
	private static String rawFileName(String catalog, String fileName, FileType fileType) {
		if (StringUtils.isBlank(catalog))
			catalog = "other";
		if (StringUtils.isBlank(fileName)) {
			fileName = UUID.randomUUID().toString().replaceAll("\\-", "")
					+ (fileType == null ? "" : fileType.getSuffix());
		} else if (fileType != null && !fileName.contains(".")) {
			fileName = fileName + fileType.getSuffix();
		}
		return new StringBuilder(catalog).append(DIR_SPLITER).append(fileName).toString();
	}
	

	private String checkPutObjectResult(PutObjectResult result){
		if(result.getResponse().isSuccessful()){
			return result.getResponse().getUri();
		}else{
			throw new RuntimeException(result.getResponse().getErrorResponseAsString());
		}
	}
}
