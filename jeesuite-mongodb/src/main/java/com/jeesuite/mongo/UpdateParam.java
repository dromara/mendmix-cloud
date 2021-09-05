/**
 * Confidential and Proprietary Copyright 2019 By 卓越里程教育科技有限公司 All Rights Reserved
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
