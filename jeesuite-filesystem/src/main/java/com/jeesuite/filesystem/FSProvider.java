/**
 * 
 */
package com.jeesuite.filesystem;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;

/**
 * 上传接口
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月5日
 */
public interface FSProvider extends Closeable {

	/**
	 * 文件方式上传
	 * @param catalog 图片分组
	 * @param fileName 文件名（需确保唯一性），为空系统自动生成
	 * @param file
	 * @return
	 */
	public String upload(String catalog,String fileName,File file);
	
	/**
	 * 字节方式上传
	 * @param catalog 图片分组
	 * @param fileName 文件名（需确保唯一性），为空系统自动生成
	 * @param data
	 * @param fileType
	 * @return
	 */
	public String upload(String catalog,String fileName,byte[] data,FileType fileType);
	
	/**
	 * 字节流方式上传
	 * @param catalog 图片分类
	 * @param fileName 文件名（需确保唯一性），为空系统自动生成
	 * @param in
	 * @param fileType
	 * @return
	 */
	public String upload(String catalog,String fileName,InputStream in,FileType fileType);
	
	
	/**
	 * 从网络地址下载图片并上传
	 * @param catalog 图片分类
	 * @param fileName fileName 文件名（需确保唯一性），为空则自动生成
	 * @param origUrl 
	 * @return
	 */
	public String upload(String catalog,String fileName,String origUrl);
	
	/**
	 * 获取图片地址
	 * @param fileName
	 * @return
	 */
	public String getPath(String fileName);
	
	/**
	 * 删除图片
	 * @return
	 */
	public boolean delete(String fileName);
	
	
	public String createUploadToken(String...fileNames);
}
