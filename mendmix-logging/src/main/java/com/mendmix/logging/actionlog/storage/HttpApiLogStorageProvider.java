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
package com.mendmix.logging.actionlog.storage;

import org.springframework.beans.factory.annotation.Value;

import com.mendmix.common.http.HttpRequestEntity;
import com.mendmix.common.model.Page;
import com.mendmix.common.model.PageParams;
import com.mendmix.common.model.PageQueryRequest;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.logging.actionlog.ActionLog;
import com.mendmix.logging.actionlog.ActionLogQueryParam;
import com.mendmix.logging.actionlog.LogStorageProvider;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2021年5月13日
 */
public class HttpApiLogStorageProvider implements LogStorageProvider {

	@Value("${mendmix.actionlog.api.baseUrl}/actionlog/add")
	private String addUrl;
	
	@Value("${mendmix.actionlog.api.baseUrl}/actionlog/list")
	private String listUrl;
	
	@Value("${mendmix.actionlog.api.baseUrl}/actionlog/details")
	private String detailsUrl;
	
	public HttpApiLogStorageProvider() {}
	
	public HttpApiLogStorageProvider(String baseUrl) {
		if(baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0,baseUrl.length() - 1);
		}
		addUrl = baseUrl + "/actionlog/add";
		listUrl = baseUrl + "/actionlog/list";
		detailsUrl = baseUrl + "/actionlog/details";
	}

	@Override
	public void storage(ActionLog log) {
		HttpRequestEntity.post(addUrl).body(log).useContext().execute();
	}

	@Override
	public Page<ActionLog> pageQuery(PageParams pageParam, ActionLogQueryParam queryParam) {
		PageQueryRequest<ActionLogQueryParam> pageQueryRequest;
		pageQueryRequest = new PageQueryRequest<>(pageParam.getPageNo(), pageParam.getPageSize(), queryParam);
		return HttpRequestEntity.post(listUrl).body(JsonUtils.toJson(pageQueryRequest)).useContext().execute().toPage(ActionLog.class);
	}

	@Override
	public ActionLog getDetails(String id) {
		return HttpRequestEntity.get(detailsUrl).queryParam("id", id).execute().toObject(ActionLog.class);
	}

}
