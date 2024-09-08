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
package test;

import org.dromara.mendmix.common.http.HttpRequestEntity;
import org.dromara.mendmix.common.http.HttpResponseEntity;
import org.dromara.mendmix.common.util.HttpUtils;

public class HttpUtilsTest {

	public static void main(String[] args) {

		HttpResponseEntity entity;
		entity = HttpUtils.get("https://www.oschina.net/news/198787/http3-rfc-9114");
		System.out.println(entity);
		
		String json = "{\"example\":{\"env\":\"dev\"},\"pageNo\":1,\"pageSize\":10}";
		entity = HttpUtils.postJson("http://openapi.mytest.com/api/commonlog/custom_log/list?_logType=mq_produce_logs", json);
		System.out.println(entity);
		
		HttpRequestEntity requestEntity = HttpRequestEntity.get("http://").basicAuth("admin", "123456");
		entity = HttpUtils.execute(requestEntity);
		System.out.println(entity);
	}

}
