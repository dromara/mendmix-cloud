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
package com.mendmix.cos.provider.local;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.mendmix.common.MendmixBaseException;
import com.mendmix.cos.BucketConfig;
import com.mendmix.cos.CObjectMetadata;
import com.mendmix.cos.CUploadObject;
import com.mendmix.cos.CUploadResult;
import com.mendmix.cos.CosProviderConfig;
import com.mendmix.cos.UploadTokenParam;
import com.mendmix.cos.provider.AbstractProvider;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Jul 30, 2022
 */
public class LocalStorageProvider extends AbstractProvider {

	public static final String NAME = "localStorage";
	
	private File baseDir;

	public LocalStorageProvider(CosProviderConfig conf) {
		super(conf);
		baseDir = new File(conf.getEndpoint());
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public boolean existsBucket(String bucketName) {
		return new File(baseDir, bucketName).exists();
	}

	@Override
	public void createBucket(String bucketName, boolean isPrivate) {
		new File(baseDir, bucketName).mkdir();
	}

	@Override
	public void deleteBucket(String bucketName) {
		new File(baseDir, bucketName).delete();
	}

	@Override
	public BucketConfig getBucketConfig(String bucketName) {
		return new BucketConfig(bucketName, false, null);
	}

	@Override
	public CUploadResult upload(CUploadObject object) {
		String fileKey = object.getFileKey();
		File destFile = getFile(object.getBucketName(), fileKey,false);
		try {
			if(object.getFile() != null){
				FileUtils.copyFile(object.getFile(), destFile);
			}else if(object.getBytes() != null){
				FileUtils.writeByteArrayToFile(destFile, object.getBytes());
			}else if(object.getInputStream() != null){
				FileUtils.writeByteArrayToFile(destFile, IOUtils.toByteArray(object.getInputStream()));
			}else{
				throw new IllegalArgumentException("upload object is NULL");
			}
			String fileUrl = getDownloadUrl(object.getBucketName(), fileKey, 0);
			return new CUploadResult(fileKey, fileUrl, null);
		} catch (Exception e) {
			e.printStackTrace();
			throw new MendmixBaseException("上传失败");
		}
	}

	@Override
	public boolean exists(String bucketName, String fileKey) {
		return new File(baseDir, fileKey).exists();
	}

	@Override
	public boolean delete(String bucketName, String fileKey) {
		return getFile(bucketName, fileKey,false).delete();
	}

	@Override
	public byte[] getObjectBytes(String bucketName, String fileKey) {
		File file = getFile(bucketName, fileKey,true);
		try {
			return FileUtils.readFileToByteArray(file);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public InputStream getObjectInputStream(String bucketName, String fileKey) {
		return new ByteArrayInputStream(getObjectBytes(bucketName, fileKey));
	}

	@Override
	public String downloadAndSaveAs(String bucketName, String fileKey, String localSaveDir) {
		return getFile(bucketName, fileKey,true).getAbsolutePath();
	}


	@Override
	public Map<String, Object> createUploadToken(UploadTokenParam param) {
		return null;
	}


	@Override
	public CObjectMetadata getObjectMetadata(String bucketName, String fileKey) {
		File file = getFile(bucketName, fileKey,true);
		CObjectMetadata metadata = new CObjectMetadata();
		metadata.setCreateTime(new Date(file.lastModified()));
		metadata.setFilesize(file.length());
		return metadata;
	}
	
	@Override
	protected String buildBucketUrlPrefix(String bucketName) {
		return null;
	}

	@Override
	protected String generatePresignedUrl(String bucketName, String fileKey, int expireInSeconds) {
		return null;
	}

	@Override
	public void close() {}
	
	private File getFile(String bucketName, String fileKey,boolean checkExists) {
		File bucketNameDir = new File(baseDir, bucketName);
		final File file = new File(bucketNameDir, fileKey);
		if(checkExists && !file.exists()) {
			throw new MendmixBaseException("文件不存在");
		}
		return file;
	}
	
}
