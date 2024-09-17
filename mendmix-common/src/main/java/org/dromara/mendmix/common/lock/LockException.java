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
package org.dromara.mendmix.common.lock;

import org.dromara.mendmix.common.MendmixBaseException;

public class LockException extends MendmixBaseException {
	private static final long serialVersionUID = 1L;

	public LockException(String e) {
		super(9999,e);
	}

	public LockException(Throwable cause) {
		super(9999, cause.getMessage(), cause);
	}
	
	
}