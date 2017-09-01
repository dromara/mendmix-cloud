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

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月5日
 */
public class UploadObject {

	private String fileName;
	private FileType fileType;
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

	public UploadObject(String fileName, InputStream inputStream, FileType fileType) {
		this.fileName = fileName;
		this.inputStream = inputStream;
		this.fileType = fileType;
	}

	public UploadObject(String fileName, byte[] bytes, FileType fileType) {
		this.fileName = fileName;
		this.bytes = bytes;
		this.fileType = fileType;
	}

	public UploadObject(String fileName, byte[] bytes) {
		this.fileName = fileName;
		this.bytes = bytes;
		this.fileType = FileType.getFileSuffix(bytes);
	}

	public String getFileName() {
		if (StringUtils.isBlank(fileName)) {
			fileName = UUID.randomUUID().toString().replaceAll("\\-", "")
					+ (fileType == null ? "" : fileType.getSuffix());
		} else if (fileType != null && !fileName.contains(".")) {
			fileName = fileName + fileType.getSuffix();
		}
		return fileName;
	}

	public FileType getFileType() {
		return fileType;
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

	public void setFileType(FileType fileType) {
		this.fileType = fileType;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	public UploadObject addMetaData(String key, Object value) {
		metadata.put(key, value);
		return this;
	}
	
	public String getMimeType(){
		return fileType != null ? fileType.getMimeType() : FileType.STREAM.getMimeType();
	}

}
