/**
 * 
 */
package com.jeesuite.filesystem.spring;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.jeesuite.filesystem.FSProvider;
import com.jeesuite.filesystem.UploadObject;
import com.jeesuite.filesystem.provider.aliyun.AliyunossProvider;
import com.jeesuite.filesystem.provider.fdfs.FdfsProvider;
import com.jeesuite.filesystem.provider.qiniu.QiniuProvider;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月7日
 */
public class FSProviderSpringFacade implements InitializingBean,DisposableBean{

	private FSProvider fsProvider;
	String endpoint;
	String provider;
	String groupName;
	String accessKey;
	String secretKey;
	String urlprefix;
	String servers;
	long connectTimeout = 3000;
	int maxThreads = 50;
	boolean privated;


	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public void setUrlprefix(String urlprefix) {
		this.urlprefix = urlprefix;
	}

	public void setServers(String servers) {
		this.servers = servers;
	}
	
	public void setPrivated(boolean privated) {
		this.privated = privated;
	}

	@Override
	public void destroy() throws Exception {
		fsProvider.close();
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		if(QiniuProvider.NAME.equals(provider)){
			Validate.notBlank(accessKey, "[accessKey] not defined");
			Validate.notBlank(secretKey, "[secretKey] not defined");
			fsProvider = new QiniuProvider(urlprefix, groupName, accessKey, secretKey,privated);
		}else if(FdfsProvider.NAME.equals(provider)){
			Validate.isTrue(servers != null && servers.matches("^.+[:]\\d{1,5}\\s*$"),"[servers] is not valid");
			String[] serversArray = servers.split(",|;");
			fsProvider = new FdfsProvider(urlprefix, groupName, serversArray, connectTimeout, maxThreads);
		}else if(AliyunossProvider.NAME.equals(provider)){
			Validate.notBlank(endpoint, "[endpoint] not defined");
			
		}else{
			throw new RuntimeException("Provider[" + provider + "] not support");
		}
	}

	public String upload(String fileName, File file) {
		return fsProvider.upload(new UploadObject(fileName, file));
	}


	public String upload(String fileName, InputStream in, String mimeType) {
		return fsProvider.upload(new UploadObject(fileName, in, mimeType));
	}

	public boolean delete(String fileName) {
		return fsProvider.delete(fileName);
	}

	public String createUploadToken(Map<String, Object> metadata,long expires,String...fileNames) {
		return fsProvider.createUploadToken(metadata, expires, fileNames);
	}

	public void setConnectTimeout(long connectTimeout) {
		this.connectTimeout = connectTimeout;
	}
	
	
	
}
