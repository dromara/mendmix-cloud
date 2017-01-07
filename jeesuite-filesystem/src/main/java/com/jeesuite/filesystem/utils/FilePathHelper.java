/**
 * 
 */
package com.jeesuite.filesystem.utils;

import com.jeesuite.filesystem.FileType;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月5日
 */
public class FilePathHelper {

	public static FileType parseFileType(String filePath){
		if(filePath.contains("/")){
			filePath = filePath.substring(filePath.lastIndexOf("/"));
		}
		filePath = filePath.split("\\?")[0];
		if(filePath.contains(".")){			
			String suffix = filePath.substring(filePath.lastIndexOf(".") + 1);
			return FileType.valueOf2(suffix);
		}
		return null;
	}
	
	public static void main(String[] args) {
		System.out.println(parseFileType("http:www.ssss.com/cccc/123.png?xxx"));
		System.out.println(parseFileType("123.png"));
		System.out.println(parseFileType("http:www.ssss.com/cccc/dtgh4r4tt/"));
	}
}
