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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mendmix.common.model.ApiInfo;
import com.mendmix.common.model.WrapperResponse;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.common.util.WebUtils;
import com.mendmix.springweb.model.AppMetadata;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年4月17日
 */
// @WebServlet(urlPatterns = "/metadata", description = "应用信息")
public class AppMetadataServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	public static final String DEFAULT_URI = "/metadata";
	
	private Map<String, String> uriSubPackageMappings = new HashMap<>();

	private static AppMetadata metadata;
	
	public void addUriSubPackageMapping(String uri,String subPackage) {
		uriSubPackageMappings.put(uri, subPackage);
	}

	@Override
	public void init() throws ServletException {
		super.init();
		metadata = AppMetadataHolder.getMetadata();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!WebUtils.isInternalRequest(req)) {
			WebUtils.responseOutJson(resp, JsonUtils.toJson(new WrapperResponse<>(403, "外网禁止访问")));
			return;
		}
		AppMetadata _metadata;
		if(uriSubPackageMappings.containsKey(req.getRequestURI())) {
			String packageName = uriSubPackageMappings.get(req.getRequestURI());
			_metadata = new AppMetadata();
			List<ApiInfo> apis = new ArrayList<>();
			for (ApiInfo apiInfo : metadata.getApis()) {
				if(apiInfo.getClassName().contains(packageName)) {
					apis.add(apiInfo);
				}
			}
			_metadata.setApis(apis);
		}else {
			_metadata = metadata;
		}
		WebUtils.responseOutJson(resp, JsonUtils.toJson(_metadata));
	}

	@Override
	public void destroy() {
		super.destroy();
	}

}
