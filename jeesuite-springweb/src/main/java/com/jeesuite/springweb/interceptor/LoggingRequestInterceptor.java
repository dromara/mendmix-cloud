package com.jeesuite.springweb.interceptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

public class LoggingRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingRequestInterceptor.class);


    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        traceRequest(request, body);
        ClientHttpResponse response = execution.execute(request, body);
        response = traceResponse(response);
        return response;
    }

    private void traceRequest(HttpRequest request, byte[] body) throws IOException {
        if(log.isTraceEnabled()){
        	StringBuilder builder = new StringBuilder();
        	builder.append("\n-----------request-----------\n");
        	builder.append("URI      :").append(request.getURI()).append("\n");
        	builder.append("Method   :").append(request.getMethod()).append("\n");
        	builder.append("Headers  :").append(request.getHeaders()).append("\n");
        	if(body != null && body.length > 0)builder.append("body     :").append(new String(body)).append("\n");
        	builder.append("-----------request end-----------\n");
            log.trace(builder.toString());
        }
    }

    private ClientHttpResponse traceResponse(ClientHttpResponse response) throws IOException {
        if(log.isTraceEnabled()){
           
        	CloneHttpResponse cloneResponse = new CloneHttpResponse(response);
        	StringBuilder builder = new StringBuilder();
        	builder.append("\n-----------response:-----------\n");
        	builder.append("Status code      :").append(cloneResponse.getStatusCode()).append("\n");
        	builder.append("Headers  :").append(cloneResponse.getHeaders()).append("\n");
        	builder.append("body     :").append(cloneResponse.bodyString()).append("\n-----------response end-----------\n");
            log.trace(builder.toString());
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