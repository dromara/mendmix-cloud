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
package com.jeesuite.common.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.jeesuite.common.http.HttpRequestEntity.FileItem;

/**
 * 
 * 
 * <br>
 * Class Name   : ApacheHttpClient
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Apr 29, 2021
 */
public class ApacheHttpClient implements HttpClientProvider {

	private static RequestConfig requestConfig;
	static {
		requestConfig = RequestConfig.custom()
				.setConnectTimeout(connectTimeout)
				.setSocketTimeout(readTimeout)
				.setConnectionRequestTimeout(readTimeout)
				.build();
	}
	@Override
	public HttpResponseEntity execute(HttpRequestEntity requestEntity)  throws IOException{
		
		CredentialsProvider  credsProvider = null;
		CloseableHttpClient httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
		CloseableHttpResponse response = null;
		try {
			HttpUriRequest request = buildHttpUriRequest(requestEntity.getUri(), requestEntity);
			response = httpClient.execute(request);
			
			HttpResponseEntity responseEntity = new HttpResponseEntity();
			responseEntity.setStatusCode(response.getStatusLine().getStatusCode());
			responseEntity.setMessage(response.getStatusLine().getReasonPhrase());
			if(response.getEntity() != null) {
				String body = EntityUtils.toString(response.getEntity(), requestEntity.getCharset());
				responseEntity.setBody(body);
			}
			return responseEntity;
		} finally {
			if(response != null)response.close();
			try {httpClient.close();} catch (IOException e) {}
			requestEntity.unset();
		}
	}
	
	
	private static HttpUriRequest buildHttpUriRequest(String url, HttpRequestEntity requestEntity) throws IOException {
	
		RequestBuilder builder = null;
		if (HttpMethod.POST == requestEntity.getMethod()) {
			builder = RequestBuilder.post().setUri(url);
		} else if (HttpMethod.GET == requestEntity.getMethod()) {
			builder = RequestBuilder.get();
		}
		
		builder.setUri(url).setConfig(requestConfig);
		
		if(requestEntity.getQueryParams() != null) {
			Set<Map.Entry<String, Object>> entrySet = requestEntity.getQueryParams().entrySet();
			for (Map.Entry<String, Object> e : entrySet) {
				if(e.getValue() == null)continue;
				builder.addParameter(e.getKey(),e.getValue().toString());
			}
		}
		
		if(requestEntity.getHeaders() != null) {
			Set<Map.Entry<String, String>> entrySet = requestEntity.getHeaders().entrySet();
			for (Map.Entry<String, String> e : entrySet) {
				if(e.getValue() == null)continue;
				builder.addHeader(e.getKey(), e.getValue());
			}
		}
		
		builder.addHeader(HttpHeaders.CONTENT_TYPE, requestEntity.getContentType());
		
		HttpEntity httpEntity = null;
		if(StringUtils.isNotBlank(requestEntity.getBody())) {
			httpEntity = new StringEntity(requestEntity.getBody(),requestEntity.getCharset() );
		}else if(requestEntity.getFormParams() != null) {
			Set<Entry<String, Object>> formEntries = requestEntity.getFormParams().entrySet();
			Object entryValue;
			if(requestEntity.isMultipart()) {
				MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
				for (Entry<String, Object> entry : formEntries) {
					entryValue = entry.getValue();
					if(entryValue == null)continue;
					if(entryValue instanceof FileItem) {
						FileItem fileItem = (FileItem)entryValue;
						ContentType contentType = null;
						if(fileItem.getMimeType() != null) {
							contentType = ContentType.create(fileItem.getMimeType());
						}
						entityBuilder.addBinaryBody(entry.getKey(), fileItem.getContent(), contentType, fileItem.getFileName());
					}else {
						entityBuilder.addTextBody(entry.getKey(), entryValue.toString());
					}
				}
				httpEntity = entityBuilder.build();
			}else {
				List <BasicNameValuePair> parameters = new ArrayList<>(formEntries.size());
				for (Entry<String, Object> entry : formEntries) {
					if(entry.getValue() == null)continue;
					parameters.add(new BasicNameValuePair(entry.getKey(),entry.getValue().toString()));
				}
				try {					
					httpEntity = new UrlEncodedFormEntity(parameters, CHARSET_UTF8);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		if(httpEntity != null)builder.setEntity(httpEntity);
		
		if(requestEntity.getBasicAuth() != null) {
			builder.addHeader(HttpHeaders.AUTHORIZATION, requestEntity.getBasicAuth().getEncodeBasicAuth());
		}

		return builder.build();
	}

}
