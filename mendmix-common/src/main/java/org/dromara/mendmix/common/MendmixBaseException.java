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
package org.dromara.mendmix.common;

import java.util.Map;

import org.dromara.mendmix.common.exception.MainErrorType;

public class MendmixBaseException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private int code;
	private String bizCode;
	
	private Map<String, String> contextParam;
	
	public MendmixBaseException() {
		super();
	}
	
	public MendmixBaseException(String message) {
		this(500, message);
	}

	public MendmixBaseException(int code,String message) {
		super(message);
		this.code = code;
	}
	
	
	public MendmixBaseException(int code, String bizCode,String message) {
		super(message);
		this.code = code;
		this.bizCode = bizCode;
	}
	
	public MendmixBaseException(MainErrorType mainErrorType) {
		this(mainErrorType.getCode(), mainErrorType.getBizCode(),mainErrorType.getMessage());
	}

	public MendmixBaseException(int code,String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getBizCode() {
		return bizCode;
	}

	public void setBizCode(String bizCode) {
		this.bizCode = bizCode;
	}

	public Map<String, String> getContextParam() {
		return contextParam;
	}

	public void setContextParam(Map<String, String> contextParam) {
		this.contextParam = contextParam;
	}
	
	

}
