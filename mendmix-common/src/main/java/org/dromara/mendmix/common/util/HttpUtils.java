/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.common.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.http.ApacheHttpClient;
import org.dromara.mendmix.common.http.HostMappingHolder;
import org.dromara.mendmix.common.http.HttpClientProvider;
import org.dromara.mendmix.common.http.HttpRequestEntity;
import org.dromara.mendmix.common.http.HttpResponseEntity;
import org.dromara.mendmix.common.http.JdkHttpClient;
import org.dromara.mendmix.common.http.OkHttp3Client;


/**
 *http操作工具类 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年8月24日
 */
public class HttpUtils {
	
	private static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.common.httpclient");
	
	private static HttpClientProvider provider;
	
	static {
		String providerType = ResourceUtils.getProperty("mendmix-cloud.httpclient.provider");
		try {
			if(providerType == null || providerType.equalsIgnoreCase("httpClient")) {
				Class.forName("org.apache.http.impl.client.CloseableHttpClient");
				Class.forName("org.apache.http.entity.mime.MultipartEntityBuilder");
				provider = new ApacheHttpClient();
			}
		} catch (Exception e) {}
		
		try {
			if(provider == null && (providerType == null || providerType.equalsIgnoreCase("okHttp3"))) {
				Class.forName("okhttp3.OkHttpClient");
				provider = new OkHttp3Client();
			}
		} catch (Exception e) {}
		
		if(provider == null) {
			provider = new JdkHttpClient();
		}
		System.out.println("init HttpClientProvider:"+provider.getClass().getSimpleName());
	}

	private HttpUtils() {}
	
	public static HttpResponseEntity get(String url) {
		return execute(HttpRequestEntity.get(url));
	}
	
	public static HttpResponseEntity postJson(String url,String json) {
		HttpRequestEntity requestEntity = HttpRequestEntity.post(url).body(json);
		return execute(requestEntity);
	}
	
	public static HttpResponseEntity postJson(String url,String json,String charset) {
		String contentType = HttpClientProvider.CONTENT_TYPE_JSON_PREFIX + charset;
		HttpRequestEntity requestEntity = HttpRequestEntity.post(url).body(json).contentType(contentType);
		return execute(requestEntity);
	}
	
	public static HttpResponseEntity execute(HttpRequestEntity requestEntity) {
		StringBuilder logBuilder = null;
		try {
			if(StringUtils.isBlank(requestEntity.getUri())) {
				throw new IllegalArgumentException("request uri is missing");
			}
			requestEntity.uri(HostMappingHolder.resolveUrl(requestEntity.getUri()));
			if(logger.isDebugEnabled()) {
				logBuilder = requestEntity.buildRequestLog();
			}
			HttpResponseEntity resp = provider.execute(requestEntity);
			if(logBuilder != null) {
				resp.appendResponseLog(logBuilder);
			}
			return resp;
		} catch (IOException e) {
			if(logBuilder != null) {
				logBuilder.append("\nexception:").append(e.getMessage());
				logBuilder.append("\n---------------backend request trace end--------------------");
			}
			if(e instanceof java.net.ConnectException) {
				return new HttpResponseEntity(503, "ConnectException:" + e.getMessage());
			}else if(e instanceof java.net.UnknownHostException) {
				return new HttpResponseEntity(400, "UnknownHostException:" + e.getMessage());
			}
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
	        	throw new MendmixBaseException(responseCode, "下载失败");
	        }
		} catch (IOException e) {
			throw new MendmixBaseException(500, "下载失败", e);
		}finally {
			try {if( outputStream!= null) outputStream.close();} catch (Exception e2) {}
			try {if( httpConn!= null) httpConn.disconnect();} catch (Exception e2) {}
		}
        
       
    }

}
