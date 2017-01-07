/**
 * 
 */
package com.jeesuite.filesystem;

import java.io.File;
import java.io.InputStream;

/**
 * 上传接口
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月5日
 */
public interface FSProvider {

	/**
	 * 文件方式上传
	 * @param catalog 图片分类
	 * @param fileKey 文件名（需确保唯一性），为空系统自动生成
	 * @param file
	 * @param fileType
	 * @return
	 */
	public String upload(String catalog,String fileKey,File file,FileType fileType);
	
	/**
	 * 字节方式上传
	 * @param catalog 图片分类
	 * @param fileKey 文件名（需确保唯一性），为空系统自动生成
	 * @param data
	 * @param fileType
	 * @return
	 */
	public String upload(String catalog,String fileKey,byte[] data,FileType fileType);
	
	/**
	 * 字节流方式上传
	 * @param catalog 图片分类
	 * @param fileKey 文件名（需确保唯一性），为空系统自动生成
	 * @param in
	 * @param fileType
	 * @return
	 */
	public String upload(String catalog,String fileKey,InputStream in,FileType fileType);
	
	
	/**
	 * 从网络地址下载图片并上传
	 * @param catalog 图片分类
	 * @param fileKey fileKey 文件名（需确保唯一性），为空则自动生成
	 * @param origUrl 
	 * @return
	 */
	public String upload(String catalog,String fileKey,String origUrl);
	
	/**
	 * 获取图片地址
	 * @param fileKey
	 * @return
	 */
	public String getPath(String fileKey);
	
	/**
	 * 删除图片
	 * @return
	 */
	public boolean delete(String fileKey);
	
	/**
	 * 文件方式上传(可覆盖旧文件)
	 * @param catalog
	 * @param fileKey
	 * @param file
	 * @param fileType
	 * @return
	 */
	public String overrideUpload(String catalog,String fileKey, File file,FileType fileType) ;
	
	/**
	 * 字节方式上传(可覆盖旧文件)
	 * @param catalog 图片分类
	 * @param fileKey 文件名（需确保唯一性），为空系统自动生成
	 * @param data
	 * @param fileType
	 * @return
	 */
	public String overrideUpload(String catalog,String fileKey,byte[] data,FileType fileType);
	
	/**
	 * 字节流方式上传(可覆盖旧文件)
	 * @param catalog 图片分类
	 * @param fileKey 文件名（需确保唯一性），为空系统自动生成
	 * @param in
	 * @param fileType
	 * @return
	 */
	public String overrideUpload(String catalog,String fileKey,InputStream in,FileType fileType);
	
	public String createUploadToken(String...fileKeys);
}
