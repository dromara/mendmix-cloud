/*
 * Copyright 2016-2022 dromara.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dromara.mendmix.cos;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.guid.GUID;
import org.dromara.mendmix.common.util.DateUtils;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月5日
 */
public class FilePathHelper {
	
	private static final String DATE_PATTERN = "yyyyMMdd";
	public static final String HTTP_PREFIX = "http://";
	public static final String HTTPS_PREFIX = "https://";
	
	public static String parseFileExtension(String filePath){
		if(filePath.contains("/")){
			filePath = filePath.substring(filePath.lastIndexOf("/"));
		}
		filePath = filePath.split("\\?")[0];
		if(filePath.contains(".")){			
			return filePath.substring(filePath.lastIndexOf(".") + 1);
		}
		return null;
	}
	
	public static String  parseFileName(String filePath){
		filePath = filePath.split("\\?")[0];
		int index = filePath.lastIndexOf("/") + 1;
		if(index > 0){
			return filePath.substring(index);
		}
		return filePath;
	}
	
	public static String getSuffix(String fileName){
		if(!fileName.contains(GlobalConstants.DOT))return StringUtils.EMPTY;
		String suffix = fileName.substring(fileName.lastIndexOf(GlobalConstants.DOT) + 1).toLowerCase();
		return suffix;
	}
	
	public static String genFileKey(String directory,String suffix,boolean withDateDir) {
		StringBuilder builder = new StringBuilder();
		if(directory != null) {
			builder.append(directory);
			if(directory.startsWith(GlobalConstants.PATH_SEPARATOR)) {
				builder.deleteCharAt(0);
			}
			if(!directory.endsWith(GlobalConstants.PATH_SEPARATOR)) {
				builder.append(GlobalConstants.PATH_SEPARATOR);
			}
		}
		if(withDateDir) {
			builder.append(DateUtils.format(new Date(), DATE_PATTERN)).append(GlobalConstants.PATH_SEPARATOR);
		}
		
		builder.append(GUID.guid());
		if(!suffix.startsWith(GlobalConstants.DOT)) {
			builder.append(GlobalConstants.DOT);
		}
		builder.append(suffix);
		
		return builder.toString();
	}
}
