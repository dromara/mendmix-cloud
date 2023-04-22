/*
 * Copyright 2016-2022 www.mendmix.com.
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
package com.mendmix.springweb.exporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.common.CustomRequestHeaders;
import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.common.util.SafeStringUtils;
import com.mendmix.common.util.TokenGenerator;
import com.mendmix.common.util.WebUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年4月17日
 */
public class RuntimeConfigServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!WebUtils.isInternalRequest(req)) {
			String authCode = StringUtils.defaultString(req.getHeader(CustomRequestHeaders.HEADER_INVOKE_TOKEN), req.getParameter("token"));
			try {					
				TokenGenerator.validate(authCode, true);
			} catch (MendmixBaseException e) {
				throw new MendmixBaseException(403, "invoke-" + e.getMessage());
			}
		}
		
		Map<String, String> result = new LinkedHashMap<String, String>();
		Properties properties = ResourceUtils.getAllProperties();
		List<String> sortKeys = new ArrayList<>();
		Set<Entry<Object, Object>> entrySet = properties.entrySet();
		for (Entry<Object, Object> entry : entrySet) {
			String key = entry.getKey().toString();
			sortKeys.add(key);
		}
		Collections.sort(sortKeys);
		String value;
		for (String key : sortKeys) {
			value = SafeStringUtils.hideSensitiveKeyValue(key, properties.getProperty(key));
			result.put(key, value);
		}
		
		WebUtils.responseOutJson(resp, JsonUtils.toJson(result));
	}

	@Override
	public void destroy() {
		super.destroy();
	}

}
