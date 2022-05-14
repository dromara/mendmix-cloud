package com.jeesuite.springboot.autoconfigure.feign;

import static feign.Util.CONTENT_ENCODING;
import static feign.Util.CONTENT_LENGTH;
import static feign.Util.ENCODING_DEFLATE;
import static feign.Util.ENCODING_GZIP;
import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.http.CustomRequestHostHolder;
import com.jeesuite.common.util.SimpleCryptUtils;
import com.jeesuite.springweb.client.RequestHeaderBuilder;

import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.Response;

/**
 * 
 * <br>
 * Class Name   : CustomLoadBalancerFeignClient
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年12月19日
 */
public class CustomLoadBalancerFeignClient implements Client{

  
	private LoadBalancerClient loadBalancer;
	
	public CustomLoadBalancerFeignClient(LoadBalancerClient loadBalancer) {
		this.loadBalancer = loadBalancer;
	}

	@Override
    public Response execute(Request request, Options options) throws IOException {
      HttpURLConnection connection = convertAndSend(request, options);
      return convertResponse(connection, request);
    }

    HttpURLConnection convertAndSend(Request request, Options options) throws IOException {
      
      String vipAddress = StringUtils.split(request.url(), "/")[1];
      String realAddress = CustomRequestHostHolder.getMapping(vipAddress);
      if(StringUtils.isBlank(realAddress)){
    	  ServiceInstance instance = loadBalancer.choose(vipAddress);
    	  if(instance == null)throw new RuntimeException("Load balancer does not have available server for client:" + vipAddress);
    	  realAddress = instance.getHost() + ":" + instance.getPort();
      }
      String url = request.url().replace(vipAddress, realAddress);
      final HttpURLConnection connection =
          (HttpURLConnection) new URL(url).openConnection();
      connection.setConnectTimeout(options.connectTimeoutMillis());
      connection.setReadTimeout(options.readTimeoutMillis());
      connection.setAllowUserInteraction(false);
      connection.setInstanceFollowRedirects(options.isFollowRedirects());
      connection.setRequestMethod(request.httpMethod().name());

      Collection<String> contentEncodingValues = request.headers().get(CONTENT_ENCODING);
      boolean gzipEncodedRequest =
          contentEncodingValues != null && contentEncodingValues.contains(ENCODING_GZIP);
      boolean deflateEncodedRequest =
          contentEncodingValues != null && contentEncodingValues.contains(ENCODING_DEFLATE);

      boolean hasAcceptHeader = false;
      Integer contentLength = null;
      for (String field : request.headers().keySet()) {
        if (field.equalsIgnoreCase("Accept")) {
          hasAcceptHeader = true;
        }
        for (String value : request.headers().get(field)) {
          if (field.equals(CONTENT_LENGTH)) {
            if (!gzipEncodedRequest && !deflateEncodedRequest) {
              contentLength = Integer.valueOf(value);
              connection.addRequestProperty(field, value);
            }
          } else {
            connection.addRequestProperty(field, value);
          }
        }
      }
      // Some servers choke on the default accept string.
      if (!hasAcceptHeader) {
        connection.addRequestProperty("Accept", "*/*");
      }
      
      //headers
      Map<String, String> customHeaders = RequestHeaderBuilder.getHeaders();
      customHeaders.forEach( (k,v) -> {
    	  connection.addRequestProperty(k, v);
      } );

     //保持原始http状态码
      connection.addRequestProperty(CustomRequestHeaders.HEADER_HTTP_STATUS_KEEP, Boolean.TRUE.toString());
     //标记不需要封装
      connection.addRequestProperty(CustomRequestHeaders.HEADER_RESP_KEEP, Boolean.TRUE.toString());
    //保持原始http状态码
      connection.addRequestProperty(CustomRequestHeaders.HEADER_CLUSTER_ID, SimpleCryptUtils.encrypt("local_test"));

      if (request.body() != null) {
        if (contentLength != null) {
          connection.setFixedLengthStreamingMode(contentLength);
        } else {
          connection.setChunkedStreamingMode(8196);
        }
        connection.setDoOutput(true);
        OutputStream out = connection.getOutputStream();
        if (gzipEncodedRequest) {
          out = new GZIPOutputStream(out);
        } else if (deflateEncodedRequest) {
          out = new DeflaterOutputStream(out);
        }
        try {
          out.write(request.body());
        } finally {
          try {
            out.close();
          } catch (IOException suppressed) { // NOPMD
          }
        }
      }
      return connection;
    }

    Response convertResponse(HttpURLConnection connection, Request request) throws IOException {
      int status = connection.getResponseCode();
      String reason = connection.getResponseMessage();

      if (status < 0) {
        throw new IOException(format("Invalid status(%s) executing %s %s", status,
            connection.getRequestMethod(), connection.getURL()));
      }

      Map<String, Collection<String>> headers = new LinkedHashMap<String, Collection<String>>();
      for (Map.Entry<String, List<String>> field : connection.getHeaderFields().entrySet()) {
        // response message
        if (field.getKey() != null) {
          headers.put(field.getKey(), field.getValue());
        }
      }

      Integer length = connection.getContentLength();
      if (length == -1) {
        length = null;
      }
      InputStream stream;
      if (status >= 400) {
        stream = connection.getErrorStream();
      } else {
        stream = connection.getInputStream();
      }
      return Response.builder()
          .status(status)
          .reason(reason)
          .headers(headers)
          .request(request)
          .body(stream, length)
          .build();
    }
  }
