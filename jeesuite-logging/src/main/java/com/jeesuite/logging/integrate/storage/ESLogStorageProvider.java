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
package com.jeesuite.logging.integrate.storage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import com.jeesuite.common.model.Page;
import com.jeesuite.common.model.PageParams;
import com.jeesuite.common.util.JsonUtils;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.logging.integrate.ActionLog;
import com.jeesuite.logging.integrate.ActionLogQueryParam;
import com.jeesuite.logging.integrate.LogStorageProvider;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年5月3日
 */
public class ESLogStorageProvider implements LogStorageProvider,InitializingBean {

	private RestHighLevelClient client;
	
	@Value("${jeesuie.actionlog.elasticsearch.index}")
    private String esindex;
	
	@Override
	public void storage(ActionLog log) {}

	@Override
	public Page<ActionLog> pageQuery(PageParams pageParam, ActionLogQueryParam queryParam) {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        buildSourceBuilder(sourceBuilder,queryParam);
        sourceBuilder.from(pageParam.offset());
        sourceBuilder.size(pageParam.getPageSize());
        
        searchRequest.source(sourceBuilder);
        try {
        	SearchResponse response = client.search(new SearchRequest(esindex).source(sourceBuilder),RequestOptions.DEFAULT);
            return convertPage(response,pageParam);
		} catch (Exception e) {
			if(e.getMessage().contains("index_not_found_exception")){
				return Page.blankPage(pageParam.getPageNo(), pageParam.getPageSize());
			}
			throw new RuntimeException(e);
		}
    }

	@Override
	public ActionLog getDetails(String id) {
		return null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		List<String> servers = ResourceUtils.getList("jeesuite.actionlog.elasticsearch.servers");
		
		HttpHost[] httpHosts = new HttpHost[servers.size()];
        for (int i = 0; i < servers.size(); i++) {
        	String[] parts = StringUtils.split(servers.get(i), ":");
        	httpHosts[i] = new HttpHost(parts[0], Integer.parseInt(parts[1]));
		}
		client = new RestHighLevelClient(RestClient.builder(httpHosts));
	}
	
	private String buildIndexName(){
    	Calendar calendar = Calendar.getInstance(); 	
    	int month = calendar.get(Calendar.MONTH) + 1;
		return esindex.replace("yyyy", String.valueOf(calendar.get(Calendar.YEAR)))
    			      .replace("MM", month > 9 ? String.valueOf(month) : ("0" + month));
    }
	
	private void buildSourceBuilder(SearchSourceBuilder sourceBuilder,ActionLogQueryParam param){
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("appId",param.getAppId()));
        if(!StringUtils.isEmpty(param.getEnv())){
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("env",param.getEnv()));
        }
        if(StringUtils.isNotBlank(param.getActionName())){
        	sourceBuilder.query(QueryBuilders.fuzzyQuery("actionName",param.getActionName()).fuzziness(Fuzziness.ZERO));
        }
        if(param.getStartTime()!=null){
            boolQueryBuilder.must(new RangeQueryBuilder("@timestamp").gt(param.getStartTime()));
        }
        if(param.getEndTime()!=null){
            boolQueryBuilder.must(new RangeQueryBuilder("@timestamp").lte(param.getEndTime()));
        }
        sourceBuilder.query(boolQueryBuilder);
        sourceBuilder.sort("@timestamp", SortOrder.DESC);
        
        sourceBuilder.timeout(new TimeValue(10000));
        sourceBuilder.trackTotalHits(true);
	}

	private Page<ActionLog> convertPage(SearchResponse response,PageParams pageParam){
        SearchHits hits = response.getHits();
        List<ActionLog> datas = new ArrayList<>();
        hits.forEach(e -> {
            datas.add(JsonUtils.toObject(e.getSourceAsString(), ActionLog.class));
        });
        Page<ActionLog> page = new Page<>(pageParam,hits.getTotalHits().value-1,datas);
        return page;
    }
}
