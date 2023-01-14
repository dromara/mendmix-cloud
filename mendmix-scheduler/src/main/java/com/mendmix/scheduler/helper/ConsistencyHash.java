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
package com.mendmix.scheduler.helper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.mendmix.common.util.DigestUtils;
import com.mendmix.scheduler.JobContext;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年8月22日
 */
public class ConsistencyHash {
	private TreeMap<Long, String> nodes = new TreeMap<Long, String>();

	// 设置虚拟节点数目
	private int VIRTUAL_NUM = 4;

	public void refresh(List<String> shards){
		nodes.clear();
		if(shards.size() > 1) {
			shards = shards.stream().sorted().collect(Collectors.toList());
		}
		for (int i = 0; i < shards.size(); i++) {
			String shardInfo = shards.get(i);
			for (int j = 0; j < VIRTUAL_NUM; j++) {
				nodes.put(hash(i + "-" + j), shardInfo);
			}
		}
	}


	/**
	 * 根据hash因子获取分配的真实节点
	 * 
	 * @param factor
	 * @return
	 */
	public String matchOneNode(Object factor) {
		if(nodes.size() <= 1)return JobContext.getContext().getNodeId();
		Long key = hash(DigestUtils.md5(factor.toString()));
		SortedMap<Long, String> tailMap = nodes.tailMap(key);
		if (tailMap.isEmpty()) {
			key = nodes.firstKey();
		} else {
			key = tailMap.firstKey();
		}
		return nodes.get(key);
	}


	private static Long hash(String key) {
        if(key == null)return 0L;
		ByteBuffer buf = ByteBuffer.wrap(key.getBytes());
		int seed = 0x1234ABCD;

		ByteOrder byteOrder = buf.order();
		buf.order(ByteOrder.LITTLE_ENDIAN);

		long m = 0xc6a4a7935bd1e995L;
		int r = 47;

		long h = seed ^ (buf.remaining() * m);

		long k;
		while (buf.remaining() >= 8) {
			k = buf.getLong();

			k *= m;
			k ^= k >>> r;
			k *= m;

			h ^= k;
			h *= m;
		}

		if (buf.remaining() > 0) {
			ByteBuffer finish = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
			finish.put(buf).rewind();
			h ^= finish.getLong();
			h *= m;
		}

		h ^= h >>> r;
		h *= m;
		h ^= h >>> r;

		buf.order(byteOrder);
		return Math.abs(h);
	}
	
	public static void main(String[] args) {
		ConsistencyHash hash = new ConsistencyHash();
		hash.refresh(new ArrayList<>(Arrays.asList("aa","bbbbbbbbbbb")));
		
		for (int i = 1; i < 11; i++) {
			System.out.println(hash.matchOneNode(i));
		}
	}

}
