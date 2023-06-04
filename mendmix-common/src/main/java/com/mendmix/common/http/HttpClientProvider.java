/*
 * Copyright 2016-2020 www.mendmix.com.
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
package com.mendmix.common.http;

import java.io.IOException;

import com.mendmix.common.util.ResourceUtils;

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
	
	int connectTimeout = ResourceUtils.getInt("mendmix.httpclient.connectTimeout", 2000);
	int readTimeout = ResourceUtils.getInt("mendmix.httpclient.readTimeout", 10000);
	String sslCipherSuites = ResourceUtils.getProperty("mendmix.httpclient.ssl.cipherSuites", "TLS");

	HttpResponseEntity execute(HttpRequestEntity requestEntity) throws IOException;
}
