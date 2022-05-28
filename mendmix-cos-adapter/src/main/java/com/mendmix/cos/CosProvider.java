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
package com.mendmix.cos;

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
