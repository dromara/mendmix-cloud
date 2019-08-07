package com.jeesuite.common.util;

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
	private List<String> uriContains = new ArrayList<>();
	private List<String> uriPrefixs = new ArrayList<>();
	private List<Pattern> uriPatterns = new ArrayList<>();

	public PathMatcher(String prefix,String uriPatterns) {
		this(prefix, Arrays.asList(StringUtils.trimToEmpty(uriPatterns).split(";|,|；")));
	}
	
	public PathMatcher(String prefix,List<String> uris) {
		if(uris == null)return;
		for (String uri : uris) {
			if(StringUtils.isBlank(uri))continue;
			if(uri.startsWith("%")){
				this.uriContains.add(uri.substring(1));
			}else{
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
		
		for (String uc : uriContains) {
			if(uri.contains(uc))return true;
		}

		for (Pattern pattern : uriPatterns) {
			if (matched = pattern.matcher(uri).matches()) {
				return true;
			}
		}

		return false;
	}
}
