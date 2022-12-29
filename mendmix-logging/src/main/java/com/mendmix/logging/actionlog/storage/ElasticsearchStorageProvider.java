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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.guid.GUID;
import com.mendmix.common.model.Page;
import com.mendmix.common.model.PageParams;
import com.mendmix.common.util.DateUtils;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.common.util.ResourceUtils;
import com.mendmix.logging.actionlog.ActionLog;
import com.mendmix.logging.actionlog.ActionLogQueryParam;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年5月3日
 */
public class ElasticsearchStorageProvider extends AbstractStorageProvider {

	private static Logger logger = LoggerFactory.getLogger("com.mendmix.logging");

	private static final String TIMESTAMP_FIELD_NAME = "timestamp";
	private static final String DATA_FORMAT = ResourceUtils.getProperty("mendmix.actionlog.elasticsearch.indexDatePattern",
			"yyyyMMdd");
	private static String actionLogIndexTpl = ResourceUtils.getProperty("mendmix.actionlog.elasticsearch.indexPrefix",
			"app_action_logs-") + "%s";
	private static String actionLogIndexFuzzyPattern = String.format(actionLogIndexTpl, "*");

	private RestHighLevelClient client;

	@Override
	public void storage(ActionLog log) {
		if(tryKafkaSend(log))return;
		String logDateString = DateUtils.format(log.getActionAt(), DATA_FORMAT);
		log.setId(logDateString + GUID.guid());
		String indexName = String.format(actionLogIndexTpl, logDateString);
		IndexRequest request = new IndexRequest().index(indexName).id(log.getId()).source(JsonUtils.toJson(log),
				XContentType.JSON);
		try {
			this.client.index(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			logger.warn("save_log_error:{}", e.getMessage());
		}
	}

	@Override
	public Page<ActionLog> pageQuery(PageParams pageParam, ActionLogQueryParam queryParam) {
		String indexName;
		if (queryParam.getStartTime() != null && queryParam.getEndTime() != null && DateUtils
				.format2DateStr(queryParam.getStartTime()).equals(DateUtils.format2DateStr(queryParam.getEndTime()))) {
			indexName = String.format(actionLogIndexTpl, DateUtils.format(queryParam.getStartTime(), DATA_FORMAT));
		} else {
			indexName = actionLogIndexFuzzyPattern;
		}

		SearchRequest searchRequest = new SearchRequest();
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		if (StringUtils.isNotBlank(queryParam.getEnv())) {
			sourceBuilder.query(QueryBuilders.termQuery("env", queryParam.getEnv()));
		}
		if (StringUtils.isNotBlank(queryParam.getAppId())) {
			sourceBuilder.query(QueryBuilders.termQuery("appId", queryParam.getAppId()));
		}
		if (StringUtils.isNotBlank(queryParam.getClientType())) {
			sourceBuilder.query(QueryBuilders.termQuery("clientType", queryParam.getClientType()));
		}
		if (StringUtils.isNotBlank(queryParam.getUserId())) {
			sourceBuilder.query(QueryBuilders.termQuery("userId", queryParam.getUserId()));
		}
		if (StringUtils.isNotBlank(queryParam.getUserName())) {
			sourceBuilder.query(QueryBuilders.termQuery("userName", queryParam.getUserName()));
		}
		if (StringUtils.isNotBlank(queryParam.getActionName())) {
			sourceBuilder.query(
					QueryBuilders.fuzzyQuery("actionName", queryParam.getActionName()).fuzziness(Fuzziness.AUTO));
		}
		if (queryParam.getSuccessed() != null) {
			sourceBuilder.query(QueryBuilders.termQuery("successed", queryParam.getSuccessed()));
		}

		if (queryParam.getStartTime() != null && queryParam.getEndTime() != null) {
			sourceBuilder.query(QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME).gte(queryParam.getStartTime().getTime())
					.lte(queryParam.getEndTime().getTime()));
		}

		sourceBuilder.from(pageParam.offset());
		sourceBuilder.size(pageParam.getPageSize());
		sourceBuilder.sort(TIMESTAMP_FIELD_NAME, SortOrder.DESC);
		sourceBuilder.timeout(new TimeValue(30000));
		searchRequest.source(sourceBuilder);
		try {
			SearchResponse response = client.search(new SearchRequest(indexName) //
					.source(sourceBuilder), RequestOptions.DEFAULT);

			if (RestStatus.OK.equals(response.status())) {
				List<ActionLog> datas = new ArrayList<>();
				SearchHits hits = response.getHits();
				for (SearchHit hit : hits) {
					datas.add(JsonUtils.toObject(hit.getSourceAsString(), ActionLog.class));
				}
				long total = hits.getTotalHits().value;
				return new Page<>(pageParam.getPageNo(), pageParam.getPageSize(), total, datas);
			}

			return Page.blankPage(pageParam.getPageNo(), pageParam.getPageSize());
		} catch (IOException e) {
			e.printStackTrace();
			throw new MendmixBaseException("查询失败:" + e.getMessage());
		}
	}

	@Override
	public ActionLog getDetails(String id) {
		if (id.length() <= DATA_FORMAT.length()) {
			return null;
		}
		String indexName = String.format(actionLogIndexTpl, id.substring(0, DATA_FORMAT.length()));
		GetRequest request = new GetRequest().index(indexName).id(id);
		GetResponse response;
		try {
			response = this.client.get(request, RequestOptions.DEFAULT);
			if (response.isExists()) {
				return JsonUtils.toObject(response.getSourceAsString(), ActionLog.class);
			}
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		List<String> servers = ResourceUtils.getList("mendmix.actionlog.elasticsearch.servers");
		HttpHost[] httpHosts = new HttpHost[servers.size()];
		for (int i = 0; i < servers.size(); i++) {
			String[] parts = StringUtils.split(servers.get(i), ":");
			httpHosts[i] = new HttpHost(parts[0], Integer.parseInt(parts[1]));
		}

		RestClientBuilder builder = RestClient.builder(httpHosts);
		if (ResourceUtils.containsProperty("mendmix.actionlog.elasticsearch.username")) {
			String username = ResourceUtils.getProperty("mendmix.actionlog.elasticsearch.username");
			String password = ResourceUtils.getProperty("mendmix.actionlog.elasticsearch.password");
			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
			builder.setHttpClientConfigCallback(
					httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
		}
		client = new RestHighLevelClient(builder);
	}

}
