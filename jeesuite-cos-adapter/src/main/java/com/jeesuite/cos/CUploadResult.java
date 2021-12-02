package com.jeesuite.cos;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 
 * <br>
 * Class Name   : UploadResult
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年1月3日
 */
public class CUploadResult {

	private String fileKey;
    private String fileUrl;
    private String mimeType;
    private long fileSize;
    private String fileHash;
	private boolean auth;

	public CUploadResult() {}

	public CUploadResult(String fileKey,String fileUrl, String fileHash) {
		super();
		this.fileKey = fileKey;
		this.fileUrl = fileUrl;
		this.fileHash = fileHash;
	}

	public String getFileKey() {
		return fileKey;
	}

	public void setFileKey(String fileKey) {
		this.fileKey = fileKey;
	}

	public String getFileUrl() {
		return fileUrl;
	}

	public void setFileUrl(String fileUrl) {
		this.fileUrl = fileUrl;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public String getFileHash() {
		return fileHash;
	}

	public void setFileHash(String fileHash) {
		this.fileHash = fileHash;
	}


	public boolean isAuth() {
		return auth;
	}

	public void setAuth(boolean auth) {
		this.auth = auth;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
   
}
