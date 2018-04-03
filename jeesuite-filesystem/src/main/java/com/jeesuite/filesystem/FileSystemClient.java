package com.jeesuite.filesystem;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.filesystem.provider.aliyun.AliyunossProvider;
import com.jeesuite.filesystem.provider.fdfs.FdfsProvider;
import com.jeesuite.filesystem.provider.qiniu.QiniuProvider;

public class FileSystemClient {
	
	private static Map<String, FileSystemClient> clients = new HashMap<>();
	private static final String PUBLIC_ID = ResourceUtils.getProperty("public.filesystem.id","public");
	private static final String PRIVATE_ID = ResourceUtils.getProperty("private.filesystem.id","private");

	private FSProvider fsProvider;
	
	private FileSystemClient(String id) {
		
		String provider = ResourceUtils.getProperty(id + ".filesystem.provider");
		Validate.notBlank(provider, "["+id+".filesystem.provider] not defined");
		boolean isPrivate = ResourceUtils.getBoolean(id + ".filesystem.private");
		String urlprefix = ResourceUtils.getProperty(id + ".filesystem.urlprefix");
		if(QiniuProvider.NAME.equals(provider)){
			String bucketName = ResourceUtils.getProperty(id + ".filesystem.bucketName");
			String accessKey = ResourceUtils.getProperty(id + ".filesystem.accessKey");
			String secretKey = ResourceUtils.getProperty(id + ".filesystem.secretKey");
			fsProvider = new QiniuProvider(urlprefix, bucketName, accessKey, secretKey,isPrivate);
		}else if(AliyunossProvider.NAME.equals(provider)){
			String endpoint = ResourceUtils.getProperty(id + ".filesystem.endpoint");
			String bucketName = ResourceUtils.getProperty(id + ".filesystem.bucketName");
			String accessKey = ResourceUtils.getProperty(id + ".filesystem.accessKey");
			String secretKey = ResourceUtils.getProperty(id + ".filesystem.secretKey");
			fsProvider = new AliyunossProvider(urlprefix,endpoint, bucketName, accessKey, secretKey,isPrivate);
		}else if(FdfsProvider.NAME.equals(provider)){
			String groupName = ResourceUtils.getProperty(id + ".filesystem.groupName");
			String servers = ResourceUtils.getProperty(id + ".filesystem.servers");
			Validate.isTrue(servers != null && servers.matches("^.+[:]\\d{1,5}\\s*$"),"[servers] is not valid");
			long connectTimeout = Long.parseLong(ResourceUtils.getProperty(id + ".filesystem.connectTimeout","3000"));
			int maxThreads = Integer.parseInt(ResourceUtils.getProperty(id + ".filesystem.maxThreads","50"));
			fsProvider = new FdfsProvider(urlprefix, groupName, servers.split(",|;"), connectTimeout, maxThreads);
		}else{
			throw new IllegalArgumentException("file provider ID:" + id + " not support");
		}
	}
	
	public static FileSystemClient getClient(String id){
		return createClient(id);
	}
	
	public static FileSystemClient getPublicClient(){
		return createClient(PUBLIC_ID);
	}
	
	public static FileSystemClient getPrivateClient(){
		return createClient(PRIVATE_ID);
	}
	
	private static FileSystemClient createClient(String id){

		FileSystemClient client = clients.get(id);
		if(client != null)return client;
		
		synchronized (clients) {
			client = clients.get(id);
			if(client != null)return client;
			client = new FileSystemClient(id);
			clients.put(id, client);
		}
		
		return client;
	}
	
	
	public String upload(File file) {
		return fsProvider.upload(new UploadObject(file));
	}
	
	public String upload(String fileKey, File file) {
		return fsProvider.upload(new UploadObject(fileKey, file));
	}
	
	public String upload(String fileKey, File file,String catalog) {
		return fsProvider.upload(new UploadObject(fileKey, file).toCatalog(catalog));
	}

	public String upload(String fileKey,byte[] contents){
		return fsProvider.upload(new UploadObject(fileKey, contents));
	}
	
	public String upload(String fileKey,byte[] contents,String catalog){
		return fsProvider.upload(new UploadObject(fileKey, contents).toCatalog(catalog));
	}

	public String upload(String fileKey, InputStream in, String mimeType) {
		return fsProvider.upload(new UploadObject(fileKey, in, mimeType));
	}
	
	public String upload(String fileKey, InputStream in, String mimeType,String catalog) {
		return fsProvider.upload(new UploadObject(fileKey, in, mimeType).toCatalog(catalog));
	}

	public boolean delete(String fileKey) {
		return fsProvider.delete(fileKey);
	}
	
	public String getDownloadUrl(String fileKey) {
		return fsProvider.getDownloadUrl(fileKey);
	}
	
	public Map<String, Object> createUploadToken(UploadTokenParam param) {
		return fsProvider.createUploadToken(param);
	}
}
