package com.jeesuite.filesystem.provider.qiniu;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.util.DateUtils;
import com.jeesuite.filesystem.FSProvider;
import com.jeesuite.filesystem.FileType;
import com.jeesuite.filesystem.utils.FilePathHelper;
import com.jeesuite.filesystem.utils.HttpDownloader;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;

/**
 * 七牛文件服务
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月5日
 */
public class QiniuProvider implements FSProvider {

	/**
	 * 
	 */
	private static final String CATALOG_SPLITER = "/";

	private static Logger logger = LoggerFactory.getLogger(QiniuProvider.class);
	
	private static final String DATE_PATTERN = "yyyyMMdd";
	
	private static UploadManager uploadManager = new UploadManager();
	private static BucketManager bucketManager;
	
	private Auth auth;
	
	private String urlprefix; 
	
	private String bucketName; 
	
	public QiniuProvider(String urlprefix,String bucketName,String accessKey, String secretKey) {
		this.urlprefix = urlprefix.endsWith(CATALOG_SPLITER) ? urlprefix : urlprefix + CATALOG_SPLITER;
		this.bucketName = bucketName;
		auth = Auth.create(accessKey, secretKey);
		
		if(bucketManager == null)bucketManager = new BucketManager(auth);
	}

	@Override
	public String upload(String catalog,String fileKey, File file,FileType fileType) {
		try {		
			fileKey = rawFileName(catalog,fileKey, fileType);
			Response res = uploadManager.put(file, fileKey,getUpToken());
			return processUploadResponse(res);
		} catch (QiniuException e) {
			processUploadException(fileKey, e);
		}
		return null;
	}

	@Override
	public String upload(String catalog,String fileKey, byte[] data,FileType fileType) {
		try {	
			if(fileType == null)fileType = FileType.getFileSuffix(data);
			
			fileKey = rawFileName(catalog,fileKey, fileType);
			Response res = uploadManager.put(data, fileKey,getUpToken());
			return processUploadResponse(res);
		} catch (QiniuException e) {
			processUploadException(fileKey, e);
		}
		return null;
	}


	@Override
	public String upload(String catalog,String fileKey, InputStream in,FileType fileType) {
		try {			
			byte[] bs = IOUtils.toByteArray(in);
			return upload(catalog,fileKey, bs,fileType);
		} catch (IOException e) {
			// TODO: handle exception
		}
		return null;
	}


	@Override
	public String upload(String catalog,String fileKey, String origUrl) {
		try {
			if(StringUtils.isBlank(fileKey)){	
				FileType fileType = FilePathHelper.parseFileType(origUrl);
				fileKey = rawFileName(catalog,fileKey, fileType);
			}else{
				fileKey = rawFileName(catalog,fileKey, null);
			}			
			
			fileKey = bucketManager.fetch(origUrl, bucketName,fileKey).key;
			
			return getFullPath(fileKey);
		} catch (QiniuException e) {
			processUploadException(fileKey, e);
		}
		
		return null;
	}
	
	@Override
	public boolean delete(String fileKey) {
		try {
			if(fileKey.contains(CATALOG_SPLITER))fileKey = fileKey.replace(urlprefix, "");
			bucketManager.delete(bucketName, fileKey);
			return true;
		} catch (QiniuException e) {
			processUploadException(fileKey, e);
		}
		return false;
	}

	@Override
	public String getPath(String fileKey) {
		try {
			String url = getFullPath(fileKey);
			if(HttpDownloader.read(url) == null){
				throw new RuntimeException("文件不存在");
			}
			return url;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private String getFullPath(String key){
		return urlprefix + key;
	}
	
	/**
	 * 处理上传结果，返回文件url
	 * @return
	 * @throws QiniuException 
	 */
	private String processUploadResponse(Response res) throws QiniuException{
		if(res.isOK()){
			UploadResult ret = res.jsonToObject(UploadResult.class);
			return getFullPath(ret.key);
		}
		throw new RuntimeException("上传错误");
	}
	
	private void processUploadException(String fileKey,QiniuException e){
        Response r = e.response;
        // 请求失败时简单状态信息
        logger.error(r.toString());
        try {logger.error(r.bodyString());} catch (QiniuException e1) {}
    
	}
	
	/**
	 * @param fileKey
	 * @param fileType
	 * @return
	 */
	private static String rawFileName(String catalog,String fileKey, FileType fileType) {
		if(StringUtils.isBlank(catalog))catalog = "other";
		if(StringUtils.isBlank(fileKey)){
			fileKey = UUID.randomUUID().toString().replaceAll("\\-", "") + (fileType == null ? "" : fileType.getSuffix());
		}else if(fileType != null && !fileKey.contains(".")){
			fileKey = fileKey + fileType.getSuffix();
		}
		//拼接目录/catalog/date/fileName
		String today = DateUtils.format(new Date(), DATE_PATTERN);
		
		return new StringBuilder(catalog).append(CATALOG_SPLITER).append(today).append(CATALOG_SPLITER).append(fileKey).toString();
	}
	
	// 简单上传，使用默认策略
	private String getUpToken(){
	    return auth.uploadToken(bucketName);
	}
	
	
	public String overrideUpload(String catalog,String fileKey, File file,FileType fileType) {
		try {		
			fileKey = rawFileName(catalog,fileKey, fileType);
			Response res = uploadManager.put(file, fileKey,auth.uploadToken(bucketName, fileKey));
			return processUploadResponse(res);
		} catch (QiniuException e) {
			processUploadException(fileKey, e);
		}
		return null;
	}

	@Override
	public String overrideUpload(String catalog, String fileKey, byte[] data, FileType fileType) {
		try {	
			if(fileType == null)fileType = FileType.getFileSuffix(data);
			
			fileKey = rawFileName(catalog,fileKey, fileType);
			Response res = uploadManager.put(data, fileKey,auth.uploadToken(bucketName, fileKey));
			return processUploadResponse(res);
		} catch (QiniuException e) {
			processUploadException(fileKey, e);
		}
		return null;
	}
	
	@Override
	public String overrideUpload(String catalog, String fileKey, InputStream in, FileType fileType) {
		try {			
			byte[] bs = IOUtils.toByteArray(in);
			return overrideUpload(catalog,fileKey, bs,fileType);
		} catch (IOException e) {
			// TODO: handle exception
		}
		return null;
	}

	@Override
	public String createUploadToken(String...fileKeys) {
		if(fileKeys != null && fileKeys.length > 0 && fileKeys[0] != null){
			return auth.uploadToken(bucketName,fileKeys[0]);
		}
		return auth.uploadToken(bucketName);
	}
	
}
