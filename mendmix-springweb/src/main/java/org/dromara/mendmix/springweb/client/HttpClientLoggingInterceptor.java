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
package org.dromara.mendmix.springweb.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.dromara.mendmix.common.util.RequestLogBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

public class HttpClientLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(HttpClientLoggingInterceptor.class);


    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        traceRequest(request, body);
        ClientHttpResponse response = execution.execute(request, body);
        response = traceResponse(response);
        return response;
    }

    private void traceRequest(HttpRequest request, byte[] body) throws IOException {
        if(log.isTraceEnabled()){
        	String requestLog = RequestLogBuilder.requestLogMessage(request.getURI().toString(), request.getMethod().name(), request.getMethodValue(), body);
            log.trace(requestLog);
        }
    }

    private ClientHttpResponse traceResponse(ClientHttpResponse response) throws IOException {
        if(log.isTraceEnabled()){         
        	CloneHttpResponse cloneResponse = new CloneHttpResponse(response);
        	String responseLog = RequestLogBuilder.responseLogMessage(cloneResponse.getStatusCode().value(),response.getHeaders(), cloneResponse.bodyString());
        	log.trace(responseLog);
            return cloneResponse;
        }
        
        return response;
    }
    
    private class CloneHttpResponse extends AbstractClientHttpResponse{
    	
    	private ClientHttpResponse response;
    	private InputStream responseStream;
    	private String bodyString;
    	
    	public CloneHttpResponse(ClientHttpResponse response) {
			this.response = response;
			try {
				bodyString = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
				responseStream = new ByteArrayInputStream(bodyString.getBytes(StandardCharsets.UTF_8.name()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public String bodyString(){
    		return bodyString;
    	}
		
		@Override
		public HttpHeaders getHeaders() {
			return response.getHeaders();
		}
		
		@Override
		public InputStream getBody() throws IOException {
			return responseStream;
		}
		
		@Override
		public String getStatusText() throws IOException {
			return response.getStatusText();
		}
		
		@Override
		public int getRawStatusCode() throws IOException {
			return response.getRawStatusCode();
		}
		
		@Override
		public void close() {
			if (this.responseStream != null) {
				try {
					StreamUtils.drain(this.responseStream);
					this.responseStream.close();
				}
				catch (IOException ex) {
					// ignore
				}
			}
		}
	}

}