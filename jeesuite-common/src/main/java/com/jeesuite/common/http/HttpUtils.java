package com.jeesuite.common.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.util.ResourceUtils;


/**
 *http操作工具类 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年8月24日
 */
public class HttpUtils {
	
	private static HttpClientProvider provider;
	
	static {
		String providerType = ResourceUtils.getProperty("jeesuite.httputil.provider");
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
		try {
			return provider.execute(url, HttpRequestEntity.create(HttpMethod.GET));
		} catch (IOException e) {
			return new HttpResponseEntity(400, e.getMessage());
		}
	}
	
	public static HttpResponseEntity postJson(String url,String json) {
		HttpRequestEntity requestEntity = HttpRequestEntity.create(HttpMethod.POST);
		try {
			requestEntity.body(json);
			return provider.execute(url, requestEntity);
		} catch (IOException e) {
			return new HttpResponseEntity(400, e.getMessage());
		}
	}
	
	public static HttpResponseEntity postJson(String url,String json,String charset) {
		String contentType = HttpClientProvider.CONTENT_TYPE_JSON_PREFIX + charset;
		HttpRequestEntity requestEntity = HttpRequestEntity.create(HttpMethod.POST).contentType(contentType);
		try {
			requestEntity.body(json);
			return provider.execute(url, requestEntity);
		} catch (IOException e) {
			return new HttpResponseEntity(400, e.getMessage());
		}
	}
	
	public static HttpResponseEntity execute(String url,HttpRequestEntity requestEntity) {
		try {
			return provider.execute(url, requestEntity);
		} catch (IOException e) {
			return new HttpResponseEntity(400, e.getMessage());
		}
	}
	
	public static HttpResponseEntity uploadFile(String url,String fieldName,File file){
		HttpRequestEntity requestEntity = HttpRequestEntity.create(HttpMethod.POST);
		try {
			requestEntity.fileParam(fieldName, file);
			return provider.execute(url, requestEntity);
		} catch (IOException e) {
			return new HttpResponseEntity(400, e.getMessage());
		}
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
