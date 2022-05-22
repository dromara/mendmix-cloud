/*
 * Copyright 2016-2020 www.jeesuite.com.
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
package com.jeesuite.mongo;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * <br>
 * Class Name   : UpdateParam
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年8月6日
 */
public class UpdateParam {

	private Map<String, Object> data;

	
	public UpdateParam(String id) {
		this.data = new HashMap<>();
		this.data.put(CommonFields.ID_FIELD_NAME, id);
	}
	
	public UpdateParam(String id,int fieldSize) {
		this.data = new HashMap<>(fieldSize + 1);
		this.data.put(CommonFields.ID_FIELD_NAME, id);
	}

	public Map<String, Object> getParams() {
		return data;
	}
	
	public UpdateParam addParam(String field,Object value){
		data.put(field, value);
		return this;
	}
	
	
}
