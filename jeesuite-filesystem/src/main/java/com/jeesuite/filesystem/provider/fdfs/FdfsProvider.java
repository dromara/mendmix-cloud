/**
 * 
 */
package com.jeesuite.filesystem.provider.fdfs;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.jeesuite.filesystem.UploadObject;
import com.jeesuite.filesystem.provider.AbstractProvider;
import com.jeesuite.filesystem.provider.FSOperErrorException;
import com.jeesuite.filesystem.sdk.fdfs.FastdfsClient;
import com.jeesuite.filesystem.sdk.fdfs.FastdfsClient.Builder;
import com.jeesuite.filesystem.sdk.fdfs.FileId;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月7日
 */
public class FdfsProvider extends AbstractProvider{

	public static final String NAME = "fastDFS";
	private FastdfsClient client;
	
	public FdfsProvider(String urlprefix, String bucketName,String[] servers,long connectTimeout,int maxThreads) {
		this.urlprefix = urlprefix.endsWith(DIR_SPLITER) ? urlprefix : urlprefix + DIR_SPLITER;
		this.bucketName = bucketName;
		Builder builder = FastdfsClient.newBuilder()
                         .connectTimeout(connectTimeout)
                         .readTimeout(connectTimeout)
                         .maxThreads(maxThreads);
		
		String[] tmpArray;
		for (String s : servers) {
			tmpArray = s.split(":");
			builder.tracker(tmpArray[0], Integer.parseInt(tmpArray[1]));
		}
		client = builder.build();
	}

	@Override
	public String upload(UploadObject object) {
		CompletableFuture<FileId> upload = null;
		try {
			if(object.getFile() != null){
				upload = client.upload(bucketName, object.getFile());
			}else if(object.getBytes() != null){
				upload = client.upload(bucketName, object.getFileName(), object.getBytes());
			}else if(object.getInputStream() != null){
				byte[] bs = IOUtils.toByteArray(object.getInputStream());
				upload = client.upload(bucketName, object.getFileName(), bs);
			}else if(StringUtils.isNotBlank(object.getUrl())){
				
			}
			return getFullPath(upload.get().toString());
		} catch (Exception e) {
			
		}
		
		return null;
	}

	@Override
	public String createUploadToken(Map<String, Object> metadata, long expires, String... fileNames) {
		return null;
	}



	@Override
	public boolean delete(String fileName) {
		try {
			if (fileName.contains(DIR_SPLITER))
				fileName = fileName.replace(urlprefix, "");
			 FileId path = FileId.fromString(fileName);
		     client.delete(path).get();
			return true;
		} catch (Exception e) {
			processException(e);
		}
		return false;
	}
	
	@Override
	public String getDownloadUrl(String file, boolean authRequire, int ttl) {
		return getFullPath(file);
	}

	
	@Override
	public String name() {
		return NAME;
	}
	
	private void processException(Exception e) {
		throw new FSOperErrorException(name(),e);
	}

	@Override
	public void close() throws IOException {
		client.close();
	}

}
