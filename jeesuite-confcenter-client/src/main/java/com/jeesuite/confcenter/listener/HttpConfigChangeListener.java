package com.jeesuite.confcenter.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.jeesuite.common.http.HttpResponseEntity;
import com.jeesuite.common.http.HttpUtils;
import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.confcenter.ConfigChangeListener;
import com.jeesuite.confcenter.ConfigcenterContext;

public class HttpConfigChangeListener implements ConfigChangeListener {

	private ScheduledExecutorService hbScheduledExecutor;

	private boolean serverInfoSynced;

	@Override
	public void register(final ConfigcenterContext context) {
		hbScheduledExecutor = Executors.newScheduledThreadPool(1);

		final String url = context.getApiBaseUrl() + "/api/sync_status";
		final Map<String, String> params = new HashMap<>();
		params.put("nodeId", context.getNodeId());
		params.put("appName", context.getApp());
		params.put("env", context.getEnv());
		hbScheduledExecutor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				// 由于初始化的时候还拿不到spring.cloud.client.ipAddress，故在同步过程上送
				if (context.isSpringboot() && serverInfoSynced == false) {
					String serverip = ResourceUtils.getProperty("spring.cloud.client.ipAddress");
					if (StringUtils.isNotBlank(serverip)) {
						params.put("serverip", serverip);
						serverInfoSynced = true;
					}
				}

				HttpResponseEntity response = HttpUtils.postJson(url, JsonUtils.toJson(params),
						HttpUtils.DEFAULT_CHARSET);
				// 刷新服务端更新的配置
				if (response.isSuccessed()) {
					JsonNode jsonNode = JsonUtils.getNode(response.getBody(), "data");
					Map map = JsonUtils.toObject(jsonNode.toString(), Map.class);
					context.updateConfig(map);
				}

			}
		}, 30, context.getSyncIntervalSeconds(), TimeUnit.SECONDS);
	}

	@Override
	public void unRegister() {
		hbScheduledExecutor.shutdown();
	}

	@Override
	public String typeName() {
		return "http";
	}

}
