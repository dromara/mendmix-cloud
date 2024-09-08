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
package org.dromara.mendmix.security;

/**
 * 
 * <br>
 * Class Name   : RequestContextAdapter
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date May 14, 2022
 */
public interface RequestContextAdapter {

	static final String _CTX_RESPONSE_KEY = "_ctx_response_key";
	static final String _CTX_REQUEST_KEY = "_ctx_request_key";
	
	String getHeader(String headerName);
	
	String getCookie(String cookieName);
	
	void addCookie(String domain,String cookieName,String cookieValue,int expire);
}
