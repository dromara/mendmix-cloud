package com.jeesuite.confcenter;

import java.util.Map;

public interface ConfigChangeHanlder {

	public void onConfigChanged(Map<String, Object> changedConfigs);
}
