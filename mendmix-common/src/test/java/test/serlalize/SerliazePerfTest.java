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
package test.serlalize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mendmix.common.serializer.JavaSerializer;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.common.util.SerializeUtils;

import test.User;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月28日
 */
public class SerliazePerfTest {

	public static void main(String[] args) throws IOException {
		JavaSerializer javaSerializer = new JavaSerializer();
		List<User> users = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			users.add(new User(i+1000, "user"+i));
		}
		
		byte[] bytes = javaSerializer.serialize(users);
		System.out.println(bytes.length);
		System.out.println(SerializeUtils.serialize(users).length);
		System.out.println(JsonUtils.toJson(users).getBytes().length);
		
		
	}
}
