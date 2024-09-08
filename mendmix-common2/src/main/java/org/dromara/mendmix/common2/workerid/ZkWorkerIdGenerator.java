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
package org.dromara.mendmix.common.workerid;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.WorkerIdGenerator;
import org.dromara.mendmix.common.util.ResourceUtils;

public class ZkWorkerIdGenerator implements WorkerIdGenerator, Watcher {

	private static final String ROOT_PATH = String.format("/applications/%s/%s/nodes", GlobalContext.ENV,GlobalContext.APPID);

	private ZooKeeper zookeeper = null;
	private volatile List<String> nodeIds;
	
	public ZkWorkerIdGenerator() {
		GlobalContext.setWorkIdGenerator(this);
	}

	public int generate(String nodeId) {
		String zkServers = ResourceUtils.getProperty("mendmix-cloud.zookeeper.servers");
		if (StringUtils.isBlank(zkServers)) {
			return 0;
		}
		try {
			zookeeper = new ZooKeeper(zkServers, 5000, this);

			Stat stat = zookeeper.exists(ROOT_PATH, false);
			if (stat == null) {
				// 创建根节点
				String[] paths = StringUtils.split(ROOT_PATH.substring(1), "/");
				String tmpPath = "";
				for (String path : paths) {
					tmpPath = tmpPath + "/" + path;
					stat = zookeeper.exists(tmpPath, false);
					if (stat == null) {
						zookeeper.create(tmpPath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
					}
				}
			}
			nodeIds = zookeeper.getChildren(ROOT_PATH, true);

			//
			String nodePath = ROOT_PATH + "/" + nodeId;
			if (!nodeIds.contains(nodeId)) {
				// 创建node节点
				zookeeper.create(nodePath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
			}
			// 等待直到当前节点创建事件下发完成
			while (!nodeIds.contains(nodeId)) {
				try {
					Thread.sleep(1);
				} catch (Exception e) {
				}
			}
			int workId = nodeIds.size();
			return workId;
		} catch (Exception e) {
			return 0;
		}
	}

	@Override
	public void process(WatchedEvent event) {
		if (event.getType() == EventType.NodeChildrenChanged) {
			try {
				nodeIds = zookeeper.getChildren(ROOT_PATH, false);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void close() {
		if (zookeeper != null) {
			try {
				zookeeper.close();
			} catch (InterruptedException e) {
			}
		}
	}

}
