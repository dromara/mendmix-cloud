/**
 * 
 */
package com.jeesuite.filesystem.provider;

import com.jeesuite.common.http.HttpUtils;
import com.jeesuite.filesystem.FSProvider;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月7日
 */
public abstract class AbstractProvider implements FSProvider{

	protected static final String URL_PREFIX_PATTERN = "(http).*\\.(com|cn)\\/";
	protected static final String HTTP_PREFIX = "http://";
	protected static final String HTTPS_PREFIX = "https://";

	protected static final String DIR_SPLITER = "/";
	
	protected String urlprefix;

	protected String bucketName;

	protected String getFullPath(String file) {
		if(file.startsWith(HTTP_PREFIX) || file.startsWith(HTTPS_PREFIX)){
			return file;
		}
		return urlprefix + file;
	}
	

	@Override
	public String downloadAndSaveAs(String file, String localSaveDir) {
		return HttpUtils.downloadFile(getDownloadUrl(file), localSaveDir);
	}

}
