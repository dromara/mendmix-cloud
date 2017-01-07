package com.jeesuite.filesystem.provider.qiniu;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.jeesuite.filesystem.FileType;
import com.jeesuite.filesystem.provider.AbstractProvider;
import com.jeesuite.filesystem.provider.FSOperErrorException;
import com.jeesuite.filesystem.utils.FilePathHelper;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;

/**
 * 七牛文件服务
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月5日
 */
public class QiniuProvider extends AbstractProvider {

	public static final String NAME = "qiniu";

	private static UploadManager uploadManager;
	private static BucketManager bucketManager;
	private Auth auth;

	public QiniuProvider(String urlprefix, String bucketName, String accessKey, String secretKey) {
		this.urlprefix = urlprefix.endsWith(DIR_SPLITER) ? urlprefix : urlprefix + DIR_SPLITER;
		this.bucketName = bucketName;
		auth = Auth.create(accessKey, secretKey);

		Zone z = Zone.autoZone();
		Configuration c = new Configuration(z);
		uploadManager = new UploadManager(c);
		bucketManager = new BucketManager(auth,c);
	}

	@Override
	public String upload(String catalog, String fileName, File file) {
		try {
			if(fileName == null)fileName = file.getName();
			fileName = rawFileName(catalog, fileName, null);
			Response res = uploadManager.put(file, fileName, getUpToken());
			return processUploadResponse(res);
		} catch (QiniuException e) {
			processUploadException(fileName, e);
		}
		return null;
	}

	@Override
	public String upload(String catalog, String fileName, byte[] data, FileType fileType) {
		try {
			if (fileType == null)
				fileType = FileType.getFileSuffix(data);

			fileName = rawFileName(catalog, fileName, fileType);
			Response res = uploadManager.put(data, fileName, getUpToken());
			return processUploadResponse(res);
		} catch (QiniuException e) {
			processUploadException(fileName, e);
		}
		return null;
	}

	@Override
	public String upload(String catalog, String fileName, InputStream in, FileType fileType) {
		try {
			byte[] bs = IOUtils.toByteArray(in);
			return upload(catalog, fileName, bs, fileType);
		} catch (IOException e) {
			throw new FSOperErrorException(name(), e);
		}
	}

	@Override
	public String upload(String catalog, String fileName, String origUrl) {
		try {
			if (StringUtils.isBlank(fileName)) {
				FileType fileType = FilePathHelper.parseFileType(origUrl);
				fileName = rawFileName(catalog, fileName, fileType);
			} else {
				fileName = rawFileName(catalog, fileName, null);
			}

			fileName = bucketManager.fetch(origUrl, bucketName, fileName).key;

			return getFullPath(fileName);
		} catch (QiniuException e) {
			processUploadException(fileName, e);
		}

		return null;
	}

	@Override
	public boolean delete(String fileName) {
		try {
			if (fileName.contains(DIR_SPLITER))
				fileName = fileName.replace(urlprefix, "");
			bucketManager.delete(bucketName, fileName);
			return true;
		} catch (QiniuException e) {
			processUploadException(fileName, e);
		}
		return false;
	}


	/**
	 * 处理上传结果，返回文件url
	 * 
	 * @return
	 * @throws QiniuException
	 */
	private String processUploadResponse(Response res) throws QiniuException {
		if (res.isOK()) {
			UploadResult ret = res.jsonToObject(UploadResult.class);
			return getFullPath(ret.key);
		}
		throw new FSOperErrorException(name(), res.toString());
	}

	private void processUploadException(String fileName, QiniuException e) {
		Response r = e.response;
		String message;
		try {
			message = r.bodyString();
		} catch (Exception e2) {
			message = r.toString();
		}
		throw new FSOperErrorException(name(), e.code(), message);
	}

	/**
	 * @param fileName
	 * @param fileType
	 * @return
	 */
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

	// 简单上传，使用默认策略
	private String getUpToken() {
		return auth.uploadToken(bucketName);
	}

	@Override
	public String createUploadToken(String... fileNames) {
		if (fileNames != null && fileNames.length > 0 && fileNames[0] != null) {
			return auth.uploadToken(bucketName, fileNames[0]);
		}
		return auth.uploadToken(bucketName);
	}

	@Override
	public String name() {
		return NAME;
	}
	
	@Override
	public void close() throws IOException {}

}
