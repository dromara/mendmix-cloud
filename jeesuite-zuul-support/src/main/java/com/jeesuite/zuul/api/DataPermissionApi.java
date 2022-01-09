package com.jeesuite.zuul.api;

import java.util.List;

import com.jeesuite.common.model.DataPermItem;

public interface DataPermissionApi {

	List<DataPermItem> findUserPermissions(String userId);
}
