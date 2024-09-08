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
package org.dromara.mendmix.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;


/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年7月25日
 */
public class PathMatcher {

	private List<String> uris = new ArrayList<>();
	private List<String> uriPrefixs = new ArrayList<>();
	private List<Pattern> uriPatterns = new ArrayList<>();

	public PathMatcher() {}

	public PathMatcher(String prefix,String uriPatterns) {
		this(prefix, Arrays.asList(StringUtils.trimToEmpty(uriPatterns).split(";|,|；")));
	}
	
	public PathMatcher(String prefix,List<String> uris) {
		if(uris == null)return;
		for (String uri : uris) {
			addUriPattern(prefix, uri);
		}
	}

	public void addUriPattern(String prefix, String uri) {
		if(StringUtils.isBlank(uri))
			return;
		uri = prefix + uri;
		if (uri.contains("*")) {
			if (uri.endsWith("*")) {
				uriPrefixs.add(uri.replaceAll("\\*+", ""));
			} else {
				this.uriPatterns.add(Pattern.compile(uri));
			}
		}else {
			this.uris.add(uri);
		}
	}

	public boolean match(String uri) {
		boolean matched = uris.contains(uri);
		if (matched)
			return matched;

		for (String prefix : uriPrefixs) {
			if (matched = uri.startsWith(prefix)) {
				return true;
			}
		}

		for (Pattern pattern : uriPatterns) {
			if (matched = pattern.matcher(uri).matches()) {
				return true;
			}
		}

		return false;
	}
}
