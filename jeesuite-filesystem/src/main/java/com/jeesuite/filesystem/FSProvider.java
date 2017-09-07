/**
 * 
 */
package com.jeesuite.filesystem;

import java.io.Closeable;
import java.util.Map;

/**
 * 上传接口
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月5日
 */
public interface FSProvider extends Closeable {

	/**
	 * 文件上传
	 * @param object
	 * @return
	 */
	public String upload(UploadObject object);
	/**
	 * 获取文件下载地址
	 * @param file 文件（全路径或者fileKey）
	 * @return
	 */
	public String getDownloadUrl(String file);
	
	/**
	 * 删除图片
	 * @return
	 */
	public boolean delete(String fileName);
	
	
	public String createUploadToken(Map<String, Object> metadata,long expires,String...fileNames);
	
	public String downloadAndSaveAs(String file,String localSaveDir);
}
