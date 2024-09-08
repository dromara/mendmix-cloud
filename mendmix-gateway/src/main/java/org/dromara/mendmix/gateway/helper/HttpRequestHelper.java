/**
 * 
 */
package org.dromara.mendmix.gateway.helper;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.springweb.client.SimpleRestTemplateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;


import reactor.core.publisher.Mono;

/**
 * <br>
 * 
 * @author vakinge
 * @date 2023年11月8日
 */
public class HttpRequestHelper {

	private static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix");
	private static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=utf-8";
	private static List<String> defalutHeaders = Arrays.asList(
			HttpHeaders.CONTENT_TYPE,
			CustomRequestHeaders.HEADER_TIMESTAMP);
	
	private static RestTemplate restTemplate = SimpleRestTemplateBuilder.build(60000,false);

	public static Mono<Void> directRequest(ServerWebExchange exchange, String targetUrl) {

		boolean traceLogging = CurrentRuntimeContext.isDebugMode();
		ServerHttpRequest request = exchange.getRequest();

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(targetUrl);
		if (!request.getQueryParams().isEmpty()) {
			request.getQueryParams().forEach((k, v) -> {
				builder.queryParam(k, v);
			});
		}
		URI uri = builder.build().encode().toUri();

		if (logger.isTraceEnabled() || traceLogging) {
			logger.info(">> proxyUri:{}", uri);
		}

		HttpHeaders headers = new HttpHeaders();
		request.getHeaders().forEach((k, v) -> {
			if (!v.isEmpty() && (defalutHeaders.contains(k) || k.startsWith(CustomRequestHeaders.HEADER_PREFIX))) {
				headers.add(k, v.get(0));
			}
		});
		if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
			headers.set(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8);
		}
		//
		if (!headers.containsKey(CustomRequestHeaders.HEADER_TIMESTAMP)) {
			headers.set(CustomRequestHeaders.HEADER_TIMESTAMP, CurrentRuntimeContext.timestamp());
		}
		headers.set(CustomRequestHeaders.HEADER_REQUEST_ID, RequestContextHelper.getRequestId(exchange));
		headers.set(HttpHeaders.CONNECTION, "close");
		String serviceChain = RequestContextHelper.buildServiceChainHeader(headers);
		headers.set(CustomRequestHeaders.HEADER_SERVICE_CHAIN, serviceChain);

		byte[] requestData = null;
		if (exchange.getRequest().getMethod() != HttpMethod.GET) {
			DataBuffer dataBuffer = exchange.getAttribute(CACHED_REQUEST_BODY_ATTR);
			if (dataBuffer != null) {
				try {
					requestData = IOUtils.toByteArray(dataBuffer.asInputStream());
				} catch (Exception e) {
					e.printStackTrace();
					throw new MendmixBaseException("读取请求数据失败");
				}
			}
		}
		if (traceLogging) {
			int contentLength = 0;
			if (requestData != null)
				contentLength = requestData.length;
			logger.info(">> proxy request \nuri:{} \n -headers:{}\n -contentLength:{}", uri, headers, contentLength);
		}

		HttpEntity<byte[]> requestEntity = new HttpEntity<byte[]>(requestData, headers);
		ResponseEntity<byte[]> responseEntity = restTemplate.exchange(uri, request.getMethod(), requestEntity,
				byte[].class);

		byte[] body = responseEntity.getBody();
		if (traceLogging) {
			logger.info(">> proxy response statusCode:{} \n -headers:{} \n -bodySize:{}",
					responseEntity.getStatusCodeValue(), responseEntity.getHeaders(), body == null ? -1 : body.length);
		}
		ServerHttpResponse response = exchange.getResponse();
		response.setRawStatusCode(responseEntity.getStatusCodeValue());
		HttpHeaders responseHeaders = responseEntity.getHeaders();
		if (responseHeaders.isEmpty()) {
			response.getHeaders().add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8);
		} else {
			response.getHeaders().addAll(responseHeaders);
		}
		if (body != null) {
			return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
		} else {
			return Mono.empty();
		}
	}
}
