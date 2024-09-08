/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.amqp;

/**
 * 
 * <br>
 * Class Name   : MessageHanlder
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2017年7月11日
 */
public interface MessageHandler {

	default boolean retrieable() {
		return false;
	}
	
	default String topicName() {
		return null;
	}
	
	default boolean actionLogging() {
		return false;
	}
	
	/**
	 * 处理消息
	 * @param message
	 * @throws Exception
	 */
	void process(MQMessage message) throws Exception;
}
