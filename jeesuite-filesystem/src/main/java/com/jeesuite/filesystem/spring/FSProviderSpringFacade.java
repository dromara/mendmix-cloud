/**
 * 
 */
package com.jeesuite.filesystem.spring;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.jeesuite.filesystem.FSProvider;
import com.jeesuite.filesystem.FileType;
import com.jeesuite.filesystem.provider.fdfs.FdfsProvider;
import com.jeesuite.filesystem.provider.qiniu.QiniuProvider;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月7日
 */
public class FSProviderSpringFacade implements InitializingBean,DisposableBean{

	private FSProvider fsProvider;
	String provider;
	String groupName;
	String accessKey;
	String secretKey;
	String urlprefix;
	String servers;
	long connectTimeout = 3000;
	int maxThreads = 50;

	public void setFsProvider(FSProvider fsProvider) {
		this.fsProvider = fsProvider;
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
	
	@Override
	public void destroy() throws Exception {
		fsProvider.close();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(!QiniuProvider.NAME.equals(provider) && !FdfsProvider.NAME.equals(provider)){
     	   throw new RuntimeException("Provider[" + provider + "] not support");
		}
		
		if(QiniuProvider.NAME.equals(provider)){
			Validate.notBlank(accessKey, "[accessKey] not defined");
			Validate.notBlank(secretKey, "[secretKey] not defined");
			fsProvider = new QiniuProvider(urlprefix, groupName, accessKey, secretKey);
		}else if(FdfsProvider.NAME.equals(provider)){
			Validate.isTrue(servers != null && servers.matches("^.+[:]\\d{1,5}\\s*$"),"[servers] is not valid");
			String[] serversArray = servers.split(",|;");
			fsProvider = new FdfsProvider(urlprefix, groupName, serversArray, connectTimeout, maxThreads);
		}
	}

	public String upload(String catalog, String fileName, File file) {
		return fsProvider.upload(catalog, fileName, file);
	}

	public String upload(String catalog, String fileName, byte[] data, FileType fileType) {
		return fsProvider.upload(catalog, fileName, data, fileType);
	}


	public String upload(String catalog, String fileName, InputStream in, FileType fileType) {
		return fsProvider.upload(catalog, fileName, in, fileType);
	}

	public String upload(String catalog, String fileName, String remoteUrl) {
		return fsProvider.upload(catalog, fileName, remoteUrl);
	}

	public boolean delete(String fileName) {
		return fsProvider.delete(fileName);
	}

	public String createUploadToken(String... fileNames) {
		return fsProvider.createUploadToken(fileNames);
	}

	public void setConnectTimeout(long connectTimeout) {
		this.connectTimeout = connectTimeout;
	}
	
	
	
}
