package com.jeesuite.cos;

import java.io.InputStream;
import java.util.Map;

/**
 * 上传接口
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月5日
 */
public interface CosProvider {

	String name();
	
	boolean existsBucket(String bucketName);
	
	void createBucket(String bucketName,boolean isPrivate);
	
	void deleteBucket(String bucketName);
	
	BucketConfig getBucketConfig(String bucketName);
	/**
	 * 文件上传
	 * @param object
	 * @return
	 */
	public CUploadResult upload(CUploadObject object);
	/**
	 * 获取文件下载地址
	 * @param file 文件（全路径或者fileKey）
	 * @return
	 */
	public String getDownloadUrl(String bucketName,String fileKey, int expireInSeconds);
	
	public boolean exists(String bucketName,String fileKey);
	/**
	 * 删除文件
	 * @return
	 */
	public boolean delete(String bucketName,String fileKey);
	
	byte[] getObjectBytes(String bucketName,String fileKey);
	
	InputStream getObjectInputStream(String bucketName,String fileKey);
	
	public String downloadAndSaveAs(String bucketName,String fileKey,String localSaveDir);
	
	public Map<String, Object> createUploadToken(UploadTokenParam param);
	
	CObjectMetadata getObjectMetadata(String bucketName,String fileKey);
	
	void close();
}
