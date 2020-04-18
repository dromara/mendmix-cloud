/**
 * Confidential and Proprietary Copyright 2019 By 卓越里程教育科技有限公司 All Rights Reserved
 */
package com.jeesuite.filesystem.provider.qcloud;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.time.DateUtils;

import com.jeesuite.filesystem.UploadObject;
import com.jeesuite.filesystem.UploadTokenParam;
import com.jeesuite.filesystem.provider.AbstractProvider;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;

/**
 * 
 * <br>
 * Class Name   : QcloudProvider
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年1月2日
 */
public class QcloudProvider extends AbstractProvider {

	public static final String NAME = "qcloud";
	
	private String bucketName;
	private String urlprefix;
	private boolean isPrivate;
	
	private COSClient cosclient;
	

	public QcloudProvider(String urlprefix,String bucketName,String accessKey, String secretKey,String regionName,boolean isPrivate) {
		
		Validate.notBlank(bucketName, "[bucketName] not defined");
		Validate.notBlank(accessKey, "[accessKey] not defined");
		Validate.notBlank(secretKey, "[secretKey] not defined");
		
		this.bucketName = bucketName;
		this.urlprefix = urlprefix.endsWith("/") ? urlprefix : (urlprefix + "/");
		this.isPrivate = isPrivate;
		
		COSCredentials cred = new BasicCOSCredentials(accessKey, secretKey);
        //设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
		if(StringUtils.isBlank(regionName))regionName = "ap-guangzhou";
        ClientConfig clientConfig = new ClientConfig(new Region(regionName));
        //生成cos客户端
        cosclient = new COSClient(cred, clientConfig);
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String upload(UploadObject object) {
		PutObjectRequest request;
		if(object.getFile() != null){
			request = new PutObjectRequest(bucketName, object.getFileName(), object.getFile());
		}else if(object.getBytes() != null){
			ByteArrayInputStream inputStream = new ByteArrayInputStream(object.getBytes());
			request = new PutObjectRequest(bucketName, object.getFileName(), inputStream, null);
		}else if(object.getInputStream() != null){
			request = new PutObjectRequest(bucketName, object.getFileName(), object.getInputStream(), null);
		}else{
			throw new IllegalArgumentException("upload object is NULL");
		}
		
		cosclient.putObject(request);
		
		return isPrivate ? object.getFileName() : urlprefix + object.getFileName();
		
	}

	@Override
	public String getDownloadUrl(String fileKey) {
		if(isPrivate){
			URL url = cosclient.generatePresignedUrl(bucketName, fileKey, DateUtils.addHours(new Date(), 1));
			return url.toString().replaceFirst(URL_PREFIX_PATTERN, urlprefix);
		}
		return urlprefix + fileKey;
	}

	@Override
	public boolean delete(String fileKey) {
		cosclient.deleteObject(bucketName, fileKey);
		return true;
	}

	//https://github.com/tencentyun/qcloud-cos-sts-sdk/tree/master/java
	@Override
	public Map<String, Object> createUploadToken(UploadTokenParam param) {
		Map<String, Object> config = new TreeMap<String, Object>();

		// 替换为您的 SecretId
		config.put("SecretId", "AKIDHTVVaVR6e3");
		// 替换为您的 SecretKey
		config.put("SecretKey", "PdkhT9e2rZCfy6");

		// 临时密钥有效时长，单位是秒，默认1800秒，最长可设定有效期为7200秒
		config.put("durationSeconds", 1800);

		// 换成您的 bucket
		config.put("bucket", "examplebucket-1250000000");
		// 换成 bucket 所在地区
		config.put("region", "ap-guangzhou");
		config.put("allowPrefix", "a.jpg");

		// 密钥的权限列表。简单上传、表单上传和分片上传需要以下的权限，其他权限列表请看
		// https://cloud.tencent.com/document/product/436/31923
		String[] allowActions = new String[] {
				// 简单上传
				"name/cos:PutObject",
				// 表单上传、小程序上传
				"name/cos:PostObject",
				// 分片上传
				"name/cos:InitiateMultipartUpload", "name/cos:ListMultipartUploads", "name/cos:ListParts",
				"name/cos:UploadPart", "name/cos:CompleteMultipartUpload" };
		config.put("allowActions", allowActions);

		//JSONObject credential = CosStsClient.getCredential(config);
		// 成功返回临时密钥信息，如下打印密钥信息
		return null;
	}

	@Override
	public void close() throws IOException {
		cosclient.shutdown();
	}

}
