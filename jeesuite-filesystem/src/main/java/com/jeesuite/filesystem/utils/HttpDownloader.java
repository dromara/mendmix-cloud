/**
 * 
 */
package com.jeesuite.filesystem.utils;

import java.io.IOException;

import com.jeesuite.filesystem.FileItem;
import com.jeesuite.filesystem.FileType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;


public class HttpDownloader {

	private static OkHttpClient httpClient = new OkHttpClient();
	
	public static FileItem read(String url) throws IOException{
		
		FileItem item = new FileItem();
		Request.Builder requestBuilder = new Request.Builder().url(url);
		Response response = httpClient.newCall(requestBuilder.build()).execute();
		
		item.setFileType(parseSuffix(url));
		item.setDatas(response.body().bytes());
		if(item.getFileType() == null){
			item.setFileType(FileType.getFileSuffix(item.getDatas()));
		}
		item.setUrl(url);
		
		return item;
	}
	
	private static FileType parseSuffix(String url){
		String sf = url.split("#|\\?")[0].substring(url.lastIndexOf("/"));
		if(!sf.contains("."))return null;
		sf = sf.substring(sf.lastIndexOf(".")+1);
		return FileType.valueOf2(sf);
	}
	
}
