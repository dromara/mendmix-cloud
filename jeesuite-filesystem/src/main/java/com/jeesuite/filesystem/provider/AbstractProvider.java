/**
 * 
 */
package com.jeesuite.filesystem.provider;

import com.jeesuite.filesystem.FSProvider;
import com.jeesuite.filesystem.utils.HttpDownloader;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月7日
 */
public abstract class AbstractProvider implements FSProvider{

	protected static final String DIR_SPLITER = "/";
	
	protected String urlprefix;

	protected String bucketName;
	
	@Override
	public String getPath(String fileName) {
		try {
			String url = getFullPath(fileName);
			if (HttpDownloader.read(url) == null) {
				throw new FSOperErrorException(name(), "文件不存在");
			}
			return url;
		} catch (Exception e) {
			throw new FSOperErrorException(name(), e);
		}
	}

	protected String getFullPath(String key) {
		return urlprefix + key;
	}
	
	public abstract String name();
}
