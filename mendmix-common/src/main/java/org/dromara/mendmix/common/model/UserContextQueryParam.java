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
package org.dromara.mendmix.common.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserContextQueryParam {

	@JsonIgnore
	private String currentTenantId;
	
	@JsonIgnore
	private String currentUserId;
	
	@JsonIgnore
	private String currentUnitId;
	
	@JsonIgnore
	Map<String, String[]> dataProfileValues;

	public String getCurrentTenantId() {
		return currentTenantId;
	}

	public void setCurrentTenantId(String currentTenantId) {
		this.currentTenantId = currentTenantId;
	}

	public String getCurrentUserId() {
		return currentUserId;
	}

	public void setCurrentUserId(String currentUserId) {
		this.currentUserId = currentUserId;
	}

	public String getCurrentUnitId() {
		return currentUnitId;
	}

	public void setCurrentUnitId(String currentUnitId) {
		this.currentUnitId = currentUnitId;
	}

	public Map<String, String[]> getDataProfileValues() {
		return dataProfileValues;
	}

	public void setDataProfileValues(Map<String, String[]> dataProfileValues) {
		this.dataProfileValues = dataProfileValues;
	}
	
	
}
