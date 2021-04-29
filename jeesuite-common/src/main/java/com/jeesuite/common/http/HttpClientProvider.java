package com.jeesuite.common.http;

import java.io.IOException;

import com.jeesuite.common.util.ResourceUtils;

/**
 * 
 * 
 * <br>
 * Class Name   : HttpClientProvider
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Apr 29, 2021
 */
public interface HttpClientProvider {
	
	String CHARSET_UTF8 = "utf-8";
	String CONTENT_ENCODING_GZIP = "gzip";
	
	String CONTENT_TYPE_JSON_PREFIX = "application/json; charset=";
	String CONTENT_TYPE_FROM_URLENCODED_PREFIX = "application/x-www-form-urlencoded; charset=";
	String CONTENT_TYPE_FROM_MULTIPART_PREFIX = "multipart/form-data;charset=";
	
	String CONTENT_TYPE_JSON_UTF8 = CONTENT_TYPE_JSON_PREFIX + CHARSET_UTF8;
	String CONTENT_TYPE_FROM_URLENCODED_UTF8 = CONTENT_TYPE_FROM_URLENCODED_PREFIX + CHARSET_UTF8;
	String CONTENT_TYPE_FROM_MULTIPART_UTF8 = CONTENT_TYPE_FROM_MULTIPART_PREFIX + CHARSET_UTF8;
	
	int connectTimeout = ResourceUtils.getInt("jeesuite.httputil.connectTimeout", 5000);
	int readTimeout = ResourceUtils.getInt("jeesuite.httputil.readTimeout", 10000);

	HttpResponseEntity execute(String uri,HttpRequestEntity requestEntity) throws IOException;
}
