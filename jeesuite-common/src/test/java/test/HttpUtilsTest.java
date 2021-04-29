package test;

import com.jeesuite.common.http.HttpMethod;
import com.jeesuite.common.http.HttpRequestEntity;
import com.jeesuite.common.http.HttpResponseEntity;
import com.jeesuite.common.http.HttpUtils;
import com.jeesuite.common.util.ResourceUtils;

public class HttpUtilsTest {

	public static void main(String[] args) {
		
		ResourceUtils.add("jeesuite.httputil.provider","httpClient");
		
		HttpResponseEntity entity;
		entity = HttpUtils.get("http://www.kuaidi100.com/query?type=yuantong&postid=11111111111");
		System.out.println(entity);
		
		String json = "{\"example\":{\"env\":\"dev\"},\"pageNo\":1,\"pageSize\":10}";
		entity = HttpUtils.postJson("http://openapi.mytest.com/api/commonlog/custom_log/list?_logType=mq_produce_logs", json);
		System.out.println(entity);
		
		HttpRequestEntity requestEntity = HttpRequestEntity.create(HttpMethod.GET).basicAuth("zyadmin", "zyadmin");
		entity = HttpUtils.execute("http://10.2.3.163:8761/", requestEntity);
		System.out.println(entity);
	}

}
