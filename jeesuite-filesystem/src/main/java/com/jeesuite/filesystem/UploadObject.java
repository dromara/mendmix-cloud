/**
 * 
 */
package com.jeesuite.filesystem;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.filesystem.utils.FilePathHelper;
import com.jeesuite.filesystem.utils.MimeTypeFileExtensionConvert;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月5日
 */
public class UploadObject {

	private String fileName;
	private String mimeType;
	private String url;
	private byte[] bytes;
	private File file;
	private InputStream inputStream;
	private Map<String, Object> metadata = new HashMap<String, Object>();

	public UploadObject(String filePath) {
		if (filePath.startsWith(FilePathHelper.HTTP_PREFIX) || filePath.startsWith(FilePathHelper.HTTPS_PREFIX)) {
			this.url = filePath;
			this.fileName = FilePathHelper.parseFileName(this.url);
		} else {
			this.file = new File(filePath);
			this.fileName = file.getName();
		}
	}

	public UploadObject(File file) {
		this.fileName = file.getName();
		this.file = file;
	}

	public UploadObject(String fileName, File file) {
		this.fileName = fileName;
		this.file = file;
	}

	public UploadObject(String fileName, InputStream inputStream, String mimeType) {
		this.fileName = fileName;
		this.inputStream = inputStream;
		this.mimeType = mimeType;
	}

	public UploadObject(String fileName, byte[] bytes, String mimeType) {
		this.fileName = fileName;
		this.bytes = bytes;
		this.mimeType = mimeType;
	}

	public UploadObject(String fileName, byte[] bytes) {
		this.fileName = fileName;
		this.bytes = bytes;
		this.mimeType = perseMimeType(bytes);
	}

	public String getFileName() {
		if (StringUtils.isBlank(fileName)) {
			fileName = UUID.randomUUID().toString().replaceAll("\\-", "");
		}
		if (mimeType != null && !fileName.contains(".")) {
			String fileExtension = MimeTypeFileExtensionConvert.getFileExtension(mimeType);
			if(fileExtension != null)fileName = fileName + fileExtension;
		}
		
		return fileName;
	}


	public String getUrl() {
		return url;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public File getFile() {
		return file;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setString(String mimeType) {
		this.mimeType = mimeType;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	public UploadObject addMetaData(String key, Object value) {
		metadata.put(key, value);
		return this;
	}
	
	public String getMimeType(){
		return mimeType;
	}

	
	private static String perseMimeType(byte[] bytes){
		try {
			MagicMatch match = Magic.getMagicMatch(bytes);
			String mimeType = match.getMimeType();
			return mimeType;
		} catch (Exception e) {
			return null;
		}
	}
}
