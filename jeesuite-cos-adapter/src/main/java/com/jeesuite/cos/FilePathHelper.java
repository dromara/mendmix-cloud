package com.jeesuite.cos;

import java.util.Calendar;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.guid.GUID;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月5日
 */
public class FilePathHelper {
	
	public static final String HTTP_PREFIX = "http://";
	public static final String HTTPS_PREFIX = "https://";
	public static final String DIR_SPLITER = "/";
	public static final String DOT = ".";
	public static final String MID_LINE = "-";
	public static final String DATE_EXPR = "{yyyy}/{MM}/{dd}/";
	
	public static enum TimeExpr {
		
		YEAR("{yyyy}"),MONTH("{MM}"),DAY("{dd}"),HOUR("{HH}"),MINUTE("{mm}"),SECOND("{ss}");

		private final String expr;
		private TimeExpr(String expr) {
			this.expr = expr;
		}
		public String getExpr() {
			return expr;
		}	
	}

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
		if(!fileName.contains(DOT))return StringUtils.EMPTY;
		String suffix = fileName.substring(fileName.lastIndexOf(DOT) + 1).toLowerCase();
		return suffix;
	}
	
	
	public static String genTimePathRandomFilePath(String suffix){
		return genFilePath(DATE_EXPR, genRandomFileName(suffix));
	}
	
	/**
	 * 
	 * @param directory 支持日期表达式如：{yyyy}/{MM}
	 * @param suffix
	 * @return
	 */
	public static String genRandomFilePath(String directory,String suffix){
		return genFilePath(directory, genRandomFileName(suffix));
	}

	
	public static String genRandomFileName(String suffix){
		StringBuilder builder = new StringBuilder();
		builder.append(GUID.guid());
		if(StringUtils.isNotBlank(suffix)) {
			if(!suffix.startsWith(DOT)) {
				builder.append(DOT);
			}
			builder.append(suffix);
		}
		return builder.toString();
	}
	
	public static String genFilePath(String folderPath,String fileKey) {
		StringBuilder builder = new StringBuilder();
		if(StringUtils.isNotBlank(folderPath)){
			builder.append(formatDirectoryPath(folderPath));
		}
		
		if(fileKey.startsWith(FilePathHelper.DIR_SPLITER)){
			builder.append(fileKey.substring(1));
		}else{
			builder.append(fileKey);
		}
		
		return builder.toString();
	}
	
	
	/**
	 * @param timeExpr
	 * @return
	 */
	public static String formatDirectoryPath(String directoryPath) {
		if(StringUtils.isBlank(directoryPath))return StringUtils.EMPTY;
		String path = directoryPath;
		Calendar calendar = Calendar.getInstance();
		if(directoryPath.contains(TimeExpr.YEAR.getExpr())){
			path = path.replace(TimeExpr.YEAR.getExpr(), String.valueOf(calendar.get(Calendar.YEAR)));
		}
		if(directoryPath.contains(TimeExpr.MONTH.getExpr())){
			int month = calendar.get(Calendar.MONTH) + 1;
			path = path.replace(TimeExpr.MONTH.getExpr(), month > 9 ? String.valueOf(month) : ("0" + month));
		}
        if(directoryPath.contains(TimeExpr.DAY.getExpr())){
        	int date = calendar.get(Calendar.DATE);
			path = path.replace(TimeExpr.DAY.getExpr(), date > 9 ? String.valueOf(date) : ("0" + date));
		}
        if(directoryPath.contains(TimeExpr.HOUR.getExpr())){
			path = path.replace(TimeExpr.HOUR.getExpr(), String.valueOf(calendar.get(Calendar.HOUR_OF_DAY)));
		}
        if(directoryPath.contains(TimeExpr.MINUTE.getExpr())){
			path = path.replace(TimeExpr.MINUTE.getExpr(), String.valueOf(calendar.get(Calendar.MINUTE)));
		}
        if(directoryPath.contains(TimeExpr.SECOND.getExpr())){
			path = path.replace(TimeExpr.SECOND.getExpr(), String.valueOf(calendar.get(Calendar.SECOND)));
		}
        
        if(directoryPath.startsWith(FilePathHelper.DIR_SPLITER)){
        	path = path.substring(1);
		}
		if(!path.endsWith(FilePathHelper.DIR_SPLITER)){	
			path = path + FilePathHelper.DIR_SPLITER;
		}
		
		return path;
	}
	
	public static void main(String[] args) {

		System.out.println(parseFileExtension("http:www.ssss.com/cccc/123.png?xxx"));
		System.out.println(parseFileExtension("123.png"));
		System.out.println(parseFileExtension("http:www.ssss.com/cccc/dtgh4r4tt/"));
		
		System.out.println(parseFileName("http:www.ssss.com/cccc/123.png?cfg"));
		
		System.out.println(genRandomFilePath("/img/{yyyy}/{MM}", "txt"));
	}
}
