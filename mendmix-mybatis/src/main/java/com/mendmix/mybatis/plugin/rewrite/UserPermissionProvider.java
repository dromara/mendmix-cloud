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
package com.mendmix.mybatis.plugin.rewrite;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.cache.CacheExpires;
import com.mendmix.cache.CacheUtils;
import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.ThreadLocalContext;
import com.mendmix.common.model.DataPermItem;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Jul 9, 2022
 */
public interface UserPermissionProvider {

	default List<DataPermItem> findCurrentAllPermissions(){
        String currentUserId = CurrentRuntimeContext.getCurrentUserId();
        if(StringUtils.isBlank(currentUserId))return new ArrayList<>(0);
        String cacheKey = "userDataPerm:" + currentUserId;
        return CacheUtils.queryTryCache(cacheKey, () -> findUserPermissions(currentUserId), CacheExpires.IN_5MINS);
	}
	
	default List<DataPermItem> findCurrentGroupPermissions(){
		String permGroup = ThreadLocalContext.getStringValue(SpecialPermissionHelper.CONTEXT_CURRENT_PERM_GROUP);
		final List<DataPermItem> allPermissions = findCurrentAllPermissions();
		if(allPermissions == null)return new ArrayList<>(0); 
		if(StringUtils.isBlank(permGroup)) {
			return allPermissions;
		}
		return allPermissions.stream().filter(o -> permGroup.equals(o.getGroupName())).collect(Collectors.toList());
	}

	List<DataPermItem> findUserPermissions(String userId);
}
