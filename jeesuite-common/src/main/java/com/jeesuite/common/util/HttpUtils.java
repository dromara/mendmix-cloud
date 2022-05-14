/*
 * Copyright 2016-2020 www.jeesuite.com.
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
package com.jeesuite.common.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.http.ApacheHttpClient;
import com.jeesuite.common.http.CustomRequestHostHolder;
import com.jeesuite.common.http.HttpClientProvider;
import com.jeesuite.common.http.HttpRequestEntity;
import com.jeesuite.common.http.HttpResponseEntity;
import com.jeesuite.common.http.JdkHttpClient;
import com.jeesuite.common.http.OkHttp3Client;


/**
 *http操作工具类 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年8月24日
 */
public class HttpUtils {
	
	private static HttpClientProvider provider;
	
	static {
		String providerType = ResourceUtils.getProperty("application.httputil.provider");
		try {
			if(providerType == null || providerType.equals("okHttp3")) {
				Class.forName("okhttp3.OkHttpClient");
				provider = new OkHttp3Client();
			}
		} catch (Exception e) {}
		if(provider == null) {
			try {
				if(providerType == null || providerType.equals("httpClient")) {
					Class.forName("org.apache.http.impl.client.CloseableHttpClient");
					Class.forName("org.apache.http.entity.mime.MultipartEntityBuilder");
					provider = new ApacheHttpClient();
				}
			} catch (Exception e) {}
		}
		
		if(provider == null) {
			provider = new JdkHttpClient();
		}
		System.out.println("==========init HttpClientProvider:"+provider.getClass().getName()+"===========");
	}

	private HttpUtils() {}
	
	public static HttpResponseEntity get(String url) {
		return execute(HttpRequestEntity.get(url));
	}
	
	public static HttpResponseEntity postJson(String url,String json) {
		HttpRequestEntity requestEntity = HttpRequestEntity.post(url);
		return execute(requestEntity);
	}
	
	public static HttpResponseEntity postJson(String url,String json,String charset) {
		String contentType = HttpClientProvider.CONTENT_TYPE_JSON_PREFIX + charset;
		HttpRequestEntity requestEntity = HttpRequestEntity.post(url).contentType(contentType);
		return execute(requestEntity);
	}
	
	public static HttpResponseEntity execute(HttpRequestEntity requestEntity) {
		try {
			if(StringUtils.isBlank(requestEntity.getUri())) {
				throw new IllegalArgumentException("request uri is missing");
			}
			String url = CustomRequestHostHolder.resolveUrl(requestEntity.getUri());
			return provider.execute(requestEntity);
		} catch (IOException e) {
			e.printStackTrace();
			return new HttpResponseEntity(400, e.getMessage());
		}
	}

	
	public static HttpResponseEntity uploadFile(String url,String fieldName,File file){
		HttpRequestEntity requestEntity = HttpRequestEntity.post(url);
		requestEntity.fileParam(fieldName, file);
		return execute(requestEntity);
	}
	
	public static String downloadFile(String fileURL, String saveDir){
		HttpURLConnection httpConn = null;
		FileOutputStream outputStream = null;
		try {
			URL url = new URL(fileURL);
	        httpConn = (HttpURLConnection) url.openConnection();
	        int responseCode = httpConn.getResponseCode();
	 
	        if (responseCode == HttpURLConnection.HTTP_OK) {
	            String fileName = "";
	            String disposition = httpConn.getHeaderField("Content-Disposition");
	 
	            if (disposition != null) {
	                int index = disposition.indexOf("filename=");
	                if (index > 0) {
	                    fileName = disposition.substring(index + 10,
	                            disposition.length() - 1);
	                }
	            } else {
	                fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
	                        fileURL.length());
	            }
	            InputStream inputStream = httpConn.getInputStream();
	            String saveFilePath = saveDir + File.separator + fileName;
	             
	            outputStream = new FileOutputStream(saveFilePath);
	 
	            int bytesRead = -1;
	            byte[] buffer = new byte[2048];
	            while ((bytesRead = inputStream.read(buffer)) != -1) {
	                outputStream.write(buffer, 0, bytesRead);
	            }
	 
	            outputStream.close();
	            inputStream.close();
	            
	            return saveFilePath;
	        } else {
	        	throw new JeesuiteBaseException(responseCode, "下载失败");
	        }
		} catch (IOException e) {
			throw new JeesuiteBaseException(500, "下载失败", e);
		}finally {
			try {if( outputStream!= null) outputStream.close();} catch (Exception e2) {}
			try {if( httpConn!= null) httpConn.disconnect();} catch (Exception e2) {}
		}
        
       
    }

}
