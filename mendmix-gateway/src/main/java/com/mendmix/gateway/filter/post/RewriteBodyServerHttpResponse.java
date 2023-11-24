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
package com.mendmix.gateway.filter.post;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ServerWebExchange;

import com.mendmix.common.CustomRequestHeaders;
import com.mendmix.gateway.GatewayConfigs;
import com.mendmix.gateway.GatewayConstants;
import com.mendmix.gateway.filter.AbstracRouteFilter;
import com.mendmix.gateway.filter.PostFilterHandler;
import com.mendmix.gateway.helper.RequestContextHelper;
import com.mendmix.gateway.model.BizSystemModule;
import com.mendmix.spring.InstanceFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年4月7日
 */
public class RewriteBodyServerHttpResponse extends ServerHttpResponseDecorator {

	private static Logger logger = LoggerFactory.getLogger("com.mendmix.gateway");

	private final String APPLICATION_JSON_STRING = MediaType.APPLICATION_JSON.toString();
	private static final String GZIP_ENCODE = "gzip";
	private static final int BODY_SIZE_LIMIT = 256 * 1024;
	private static final int LOGGING_BODY_SIZE_LIMIT = 100 * 1024;
	
	private static List<HttpMessageReader<?>> messageReaders;
	private static List<PostFilterHandler> handlers;
	
	private ServerWebExchange exchange;
	private BizSystemModule module;
	private String bodyString;
	
	public RewriteBodyServerHttpResponse(ServerWebExchange exchange, BizSystemModule module) {
		super(exchange.getResponse());
		this.exchange = exchange;
		this.module = module;
	}
	
	public static void setHandlers(List<PostFilterHandler> handlers) {
		if (handlers.size() > 1) {
			handlers = handlers.stream().sorted(Comparator.comparing(PostFilterHandler::order)).collect(Collectors.toList());
		}
		RewriteBodyServerHttpResponse.handlers = handlers;
	}
	
	private static List<HttpMessageReader<?>> getMessageReaders() {
		if(messageReaders == null) {
			//messageReaders = HandlerStrategies.withDefaults().messageReaders();
			messageReaders = InstanceFactory.getInstance(ServerCodecConfigurer.class).getReaders();
		}
		return messageReaders;
	}

	public String getBodyString() {
		return bodyString;
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		final HttpHeaders headers = exchange.getResponse().getHeaders();
    	if (headers.containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
    		AbstracRouteFilter.addIgnoreResponseFilterUri(exchange);
    		return super.writeWith(body);
		}
    	
    	//
		final long contentLength = headers.getContentLength();
		final boolean withTransferEncodingHeader = headers.containsKey(HttpHeaders.TRANSFER_ENCODING);
		boolean isNullBody = !withTransferEncodingHeader && contentLength == 0;
		if(isNullBody) {
			for (PostFilterHandler handler : handlers) {
				bodyString = handler.process(exchange, module, bodyString);
			}
			if(bodyString != null) {
				byte[] bytes = bodyString.getBytes();
				DataBuffer dataBuffer = exchange.getResponse().bufferFactory().wrap(bytes);
				headers.setContentLength(bytes.length);
				return super.writeWith(Flux.just(dataBuffer));
			}
			return super.writeWith(body);
		}
		//
		if(GatewayConfigs.actionLogEnabled && !GatewayConfigs.actionResponseBodyIngore && headers.containsKey(CustomRequestHeaders.HEADER_EXCEPTION_CODE)) {
			exchange.getAttributes().put(GatewayConstants.CONTEXT_ACTIVE_LOG_BODY, true);
		}

		final boolean isGzip = GZIP_ENCODE.equalsIgnoreCase(headers.getFirst(HttpHeaders.CONTENT_ENCODING));
        if(logger.isTraceEnabled()) {
        	logger.trace("handleReadResponseBody begin -> isGzip:{}",isGzip);
        }
        String originalResponseContentType = exchange.getAttribute(ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR);
		if (originalResponseContentType != null && originalResponseContentType.startsWith(APPLICATION_JSON_STRING)){
            ClientResponse clientResponse = ClientResponse
                    .create(this.getDelegate().getStatusCode(), getMessageReaders())
                    .body(Flux.from(body)).build();

            Mono<byte[]> bodyMono = clientResponse.bodyToMono(byte[].class).map((bytes) -> {
            	if(isGzip) {
            		bytes = gzipDecode(bytes);
            	}
            	if(bytes.length >= LOGGING_BODY_SIZE_LIMIT) {
            		exchange.getAttributes().put(GatewayConstants.CONTEXT_IGNORE_LOG_BODY, true);
            	}
            	bodyString = new String(bytes, StandardCharsets.UTF_8);
            	for (PostFilterHandler handler : handlers) {
    				bodyString = handler.process(exchange, module, bodyString);
    			}
            	bytes = bodyString.getBytes();
            	if(isGzip) {
            		bytes = gzipEncode(bodyString.getBytes());
            	}
            	
            	int newContentLength = bytes.length;
            	if(contentLength <= 0) {
            		exchange.getAttributes().put(GatewayConstants.CONTEXT_RESP_CONTENT_LENGTH, newContentLength);
            	}
            	//
            	if((headers.containsKey(CustomRequestHeaders.HEADER_RESP_KEEP)) 
            			&& newContentLength > BODY_SIZE_LIMIT) {
            		AbstracRouteFilter.addIgnoreResponseFilterUri(exchange);
            	}
                return bytes;
            });
            
            BodyInserter<Mono<byte[]>, ReactiveHttpOutputMessage> bodyInserter = BodyInserters.fromPublisher(bodyMono,byte[].class);
            CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(
                    exchange, exchange.getResponse().getHeaders());
            return bodyInserter.insert(outputMessage, new BodyInserterContext())
                    .then(Mono.defer(() -> {
                        Flux<DataBuffer> messageBody = outputMessage.getBody();
                        //HttpHeaders headers = getDelegate().getHeaders();
                        if (!withTransferEncodingHeader) {
                            messageBody = messageBody.doOnNext(data -> headers
                                    .setContentLength(data.readableByteCount()));
                        }
                        // TODO: fail if isStreamingMediaType?
                        return getDelegate().writeWith(messageBody);
                    }));
        }else {
        	if(RequestContextHelper.getCurrentApi(exchange) != null) {        		
        		AbstracRouteFilter.addIgnoreResponseFilterUri(exchange);
        	}
            return super.writeWith(body);
        }
	}

	private static byte[] gzipDecode(byte[] encoded) {
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
			GZIPInputStream gis = new GZIPInputStream(bis);
			return FileCopyUtils.copyToByteArray(gis);
		} catch (IOException e) {
			throw new IllegalStateException("couldn't decode body from gzip", e);
		}
	}

	private static byte[] gzipEncode(byte[] original) {
		try {
			ByteArrayOutputStream bis = new ByteArrayOutputStream();
			GZIPOutputStream gos = new GZIPOutputStream(bis);
			FileCopyUtils.copy(original, gos);
			return bis.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("couldn't encode body to gzip", e);
		}
	}
}
