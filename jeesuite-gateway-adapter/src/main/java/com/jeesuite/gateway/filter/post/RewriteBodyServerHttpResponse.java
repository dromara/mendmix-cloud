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
package com.jeesuite.gateway.filter.post;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.server.ServerWebExchange;

import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.model.ApiInfo;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.gateway.filter.PostFilterHandler;
import com.jeesuite.gateway.model.BizSystemModule;
import com.jeesuite.springweb.GlobalConfigs;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年4月7日
 */
public class RewriteBodyServerHttpResponse extends ServerHttpResponseDecorator {

	private static Logger logger = LoggerFactory.getLogger("com.zvosframework.adapter.gateway");

	private static final String GZIP_ENCODE = "gzip";

	private static List<PostFilterHandler> handlers = new ArrayList<>();

	private ServerWebExchange exchange;
	private BizSystemModule module;
	private String bodyString;
	private boolean rewriteEnabled = ResourceUtils.getBoolean("filter.response.rewrite.enbaled", true);

	public static void setHandlers(List<PostFilterHandler> handlers) {
		if (handlers.size() > 1) {
			handlers.stream().sorted(Comparator.comparing(PostFilterHandler::order));
		}
		RewriteBodyServerHttpResponse.handlers = handlers;
	}

	public RewriteBodyServerHttpResponse(ServerWebExchange exchange, BizSystemModule module) {
		super(exchange.getResponse());
		this.exchange = exchange;
		this.module = module;
	}

	public String getBodyString() {
		return bodyString;
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {

		HttpHeaders headers = exchange.getResponse().getHeaders();
		if (headers.containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
			return super.writeWith(body);
		}

		MediaType contentType = headers.getContentType();
		if (contentType == null || !contentType.getType().equals(MediaType.APPLICATION_JSON.getType())) {
			return super.writeWith(body);
		}
		
		boolean buildNewResponse = rewriteEnabled && !module.isBodyRewriteIgnore();
		//
		if(buildNewResponse) {
			buildNewResponse = !headers.containsKey(CustomRequestHeaders.HEADER_RESP_KEEP);
		}
		//
		if(buildNewResponse) {
			buildNewResponse = !exchange.getRequest().getHeaders().containsKey(CustomRequestHeaders.HEADER_RESP_KEEP);
		}

		if (!buildNewResponse && GlobalConfigs.requestLogEnabled) {
			ApiInfo apiInfo = module.getApiInfo(exchange.getRequest().getPath().value());
			buildNewResponse = apiInfo != null && apiInfo.isActionLog() && apiInfo.isResponseLog();
		}

		if (!buildNewResponse) {
			return super.writeWith(body);
		}

		DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
		if (body instanceof Flux) {
			Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
			return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
				DataBuffer dataBuffer = bufferFactory.join(dataBuffers);
				byte[] content = handleReadResponseBody(dataBuffer);
				return bufferFactory.wrap(content);
			}));
		}
		return super.writeWith(body);
	}

	private byte[] handleReadResponseBody(DataBuffer dataBuffer) {
		try {
			HttpHeaders headers = exchange.getResponse().getHeaders();
			boolean isGzip = GZIP_ENCODE.equalsIgnoreCase(headers.getFirst(HttpHeaders.CONTENT_ENCODING));
            if(logger.isDebugEnabled()) {
            	logger.debug("handleReadResponseBody begin -> isGip:{}",isGzip);
            }
			byte[] content = new byte[dataBuffer.readableByteCount()];
			dataBuffer.read(content);
			if (isGzip) {
				content = gzipDecode(content);
			}
			bodyString = new String(content, StandardCharsets.UTF_8);
			//
			for (PostFilterHandler handler : handlers) {
				bodyString = handler.process(exchange, module, bodyString);
			}
			byte[] bytes = bodyString.getBytes();
			if (isGzip) {
				bytes = gzipEncode(bytes);
			}
			if (!headers.containsKey(HttpHeaders.TRANSFER_ENCODING)
					|| headers.containsKey(HttpHeaders.CONTENT_LENGTH)) {
				headers.setContentLength(bytes.length);
			}
			return bytes;
		} finally {
			DataBufferUtils.release(dataBuffer);
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
