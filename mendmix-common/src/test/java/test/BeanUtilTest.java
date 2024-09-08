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
package test;

import org.dromara.mendmix.common.util.BeanUtils;
import org.dromara.mendmix.common.util.JsonUtils;

public class BeanUtilTest {

	public static void main(String[] args) {
		User user = new User();
		user.setName("小伙子");
		user.setMobile("13800138000");
		user.setFather(new User(1000, "你爹"));
		
		BaseUser user2 = BeanUtils.copy(user, BaseUser.class);
		System.out.println(JsonUtils.toPrettyJson(user2));
	}
}
