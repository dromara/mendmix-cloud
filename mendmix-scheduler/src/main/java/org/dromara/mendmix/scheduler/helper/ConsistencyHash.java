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
package org.dromara.mendmix.scheduler.helper;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.dromara.mendmix.common.util.HashUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年8月22日
 */
public class ConsistencyHash {
	private TreeMap<Long, String> nodes = new TreeMap<Long, String>();

	// 设置虚拟节点数目
	private int virtualNum = 4;
	
	public int getVirtualNum() {
		return virtualNum;
	}

	public ConsistencyHash() {
		this(4);
	}
	
	public ConsistencyHash(int virtualNum) {
		this.virtualNum = virtualNum;
	}


	public void refresh(List<String> shards){
		nodes.clear();
		if(shards.size() > 1) {
			shards = shards.stream().sorted().collect(Collectors.toList());
		}
		for (int i = 0; i < shards.size(); i++) {
			String shardInfo = shards.get(i);
			for (int j = 0; j < virtualNum; j++) {
				nodes.put(HashUtils.hash(i + "-" + j), shardInfo);
			}
		}
	}


	/**
	 * 根据hash因子获取分配的真实节点
	 * 
	 * @param factor
	 * @return
	 */
	public String matchOne(String factor,String defaultVal) {
		if(nodes.size() <= 1)return defaultVal;
		Long hash = HashUtils.hash(factor);
		SortedMap<Long, String> tailMap = nodes.tailMap(hash);
		Long key;
		if (tailMap.isEmpty()) {
			key = nodes.firstKey();
		} else {
			key = tailMap.firstKey();
		}
		return nodes.get(key);
	}


}
