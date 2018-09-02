/**
 * 
 */
package com.jeesuite.filesystem.provider.fdfs;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.csource.common.NameValuePair;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;

import com.jeesuite.filesystem.UploadObject;
import com.jeesuite.filesystem.UploadTokenParam;
import com.jeesuite.filesystem.provider.AbstractProvider;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月7日
 */
public class FdfsProvider extends AbstractProvider {

	public static final String NAME = "fastDFS";

	private String groupName;
	private StorageClient1 client;

	public FdfsProvider(String groupName, Properties props) {
		this.groupName = groupName;
		try {
			ClientGlobal.initByProperties(props);
			TrackerClient tracker = new TrackerClient();
			TrackerServer trackerServer = tracker.getConnection();
			StorageServer storageServer = tracker.getStoreStorage(trackerServer);
			client = new StorageClient1(trackerServer, storageServer);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String upload(UploadObject object) {
		NameValuePair[] metaDatas = new NameValuePair[object.getMetadata().size()];
		int index = 0;
		for (String key : object.getMetadata().keySet()) {
			metaDatas[index++] = new NameValuePair(key, object.getMetadata().get(key).toString());
		}
		try {
			if (object.getFile() != null) {
				client.upload_file1(groupName, object.getFile().getAbsolutePath(), object.getMimeType(), metaDatas);
			} else if (object.getBytes() != null) {

			} else if (object.getInputStream() != null) {

			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}

	@Override
	public Map<String, Object> createUploadToken(UploadTokenParam param) {
		return null;
	}

	@Override
	public boolean delete(String fileKey) {
		return false;
	}

	@Override
	public String getDownloadUrl(String fileKey) {
		return getFullPath(fileKey);
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public void close() throws IOException {

	}
}
