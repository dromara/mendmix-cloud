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
package org.dromara.mendmix.mybatis.plugin.rewrite;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.cache.CacheExpires;
import org.dromara.mendmix.cache.CacheUtils;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.model.DataPermItem;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Aug 3, 2024
 */
public interface UserPermissionProvider {

	default List<DataPermItem> findCurrentAllPermissions(){
		String systemId = CurrentRuntimeContext.getSystemId();
		String tenantId = CurrentRuntimeContext.getTenantId();
        String currentUserId = CurrentRuntimeContext.getCurrentUserId();
        if(StringUtils.isBlank(currentUserId))return new ArrayList<>(0);
        String cacheKey = StringUtils.join("userDataPerm:",StringUtils.trimToEmpty(systemId),GlobalConstants.UNDER_LINE,StringUtils.trimToEmpty(tenantId),GlobalConstants.UNDER_LINE,currentUserId);
        return CacheUtils.queryTryCache(cacheKey, () -> findUserPermissions(systemId,tenantId,currentUserId), CacheExpires.IN_5MINS);
	}
	
	default List<DataPermItem> findCurrentGroupPermissions(){
		String permGroup = ThreadLocalContext.getStringValue(CustomRequestHeaders.HEADER_REFERER_PERM_GROUP);
		final List<DataPermItem> allPermissions = findCurrentAllPermissions();
		if(allPermissions == null)return new ArrayList<>(0); 
		if(StringUtils.isBlank(permGroup)) {
			return allPermissions;
		}
		return allPermissions.stream().filter(o -> permGroup.equals(o.getGroupName())).collect(Collectors.toList());
	}

	List<DataPermItem> findUserPermissions(String systemId,String tenantId,String userId);
}
