/*
 * Copyright 2016-2022 www.jeesuite.com.
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
package com.jeesuite.gateway.zuul.filter.post;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.google.common.io.CharStreams;
import com.jeesuite.common.CustomRequestHeaders;
import com.netflix.util.Pair;
import com.netflix.zuul.context.RequestContext;

/**
 * 
 * <br>
 * Class Name : ResponseCompose
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Feb 26, 2022
 */
public class ResponseCompose {

	private String bodyString;
	private boolean gzipEnabled;
	private boolean rewriteEnabled = true;
	private boolean successed;
	private int statusCode;

	public ResponseCompose(RequestContext ctx) {
		List<Pair<String, String>> headers = ctx.getOriginResponseHeaders();
		gzipEnabled = ctx.getResponseGZipped();
		statusCode = ctx.getResponseStatusCode();
		if (successed = statusCode == 200) {
			boolean readBody = true;
			for (Pair<String, String> pair : headers) {
				if (CustomRequestHeaders.HEADER_RESP_KEEP.equals(pair.first())) {
					rewriteEnabled = !Boolean.parseBoolean(pair.second());
				}
				//
				if (readBody && HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(pair.first())
						&& !pair.second().contains(MediaType.APPLICATION_JSON_VALUE)
						&& !pair.second().contains(MediaType.TEXT_PLAIN_VALUE)
						&& !pair.second().contains(MediaType.TEXT_HTML_VALUE)) {
					readBody = false;
				}

				if (readBody && HttpHeaders.CONTENT_DISPOSITION.equalsIgnoreCase(pair.first())) {
					readBody = false;
				}
			}
			//
			if (readBody) {
				// TODO gzip
				InputStream responseDataStream = ctx.getResponseDataStream();
				if (responseDataStream != null) {
					try {
						bodyString = CharStreams.toString(new InputStreamReader(responseDataStream, StandardCharsets.UTF_8));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}

	public String getBodyString() {
		return bodyString;
	}

	public boolean isGzipEnabled() {
		return gzipEnabled;
	}

	public boolean isRewriteEnabled() {
		return rewriteEnabled && bodyString != null;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public boolean isSuccessed() {
		return successed;
	}

}
