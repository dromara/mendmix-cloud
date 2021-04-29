package com.jeesuite.common.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

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
	public HttpResponseEntity execute(String uri, HttpRequestEntity requestEntity)  throws IOException{
		
		CredentialsProvider  credsProvider = null;
//		if(requestEntity.getBasicAuth() != null) {
//			credsProvider = new BasicCredentialsProvider();
//			Credentials credentials = new UsernamePasswordCredentials(requestEntity.getBasicAuth().getName(),requestEntity.getBasicAuth().getPassword());
//			credsProvider.setCredentials(org.apache.http.auth.AuthScope.ANY,credentials);
//		}
        
		CloseableHttpClient httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
		CloseableHttpResponse response = null;
		try {
			HttpUriRequest request = buildHttpUriRequest(uri, requestEntity);
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
		}
	}
	
	
	private static HttpUriRequest buildHttpUriRequest(String url, HttpRequestEntity requestEntity) {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		if(requestEntity.getQueryParams() != null) {
			Set<Map.Entry<String, Object>> entrySet = requestEntity.getQueryParams().entrySet();
			for (Map.Entry<String, Object> e : entrySet) {
				if(e.getValue() == null)continue;
				NameValuePair pair = new BasicNameValuePair(e.getKey(),  e.getValue().toString());
				params.add(pair);
			}
		}
		
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
		
		if(StringUtils.isNotBlank(requestEntity.getBody())) {
			builder.setEntity(new StringEntity(requestEntity.getBody(),requestEntity.getCharset() ));
		}
		
		if(requestEntity.getBasicAuth() != null) {
			builder.addHeader(HttpHeaders.AUTHORIZATION, requestEntity.getBasicAuth().getEncodeBasicAuth());
		}

		return builder.build();
	}

}
