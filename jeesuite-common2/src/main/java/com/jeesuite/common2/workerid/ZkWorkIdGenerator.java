package com.jeesuite.common2.workerid;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.WorkIdGenerator;
import com.jeesuite.common.util.ResourceUtils;

public class ZkWorkIdGenerator implements WorkIdGenerator, Watcher {

	private static final String ROOT_PATH = String.format("/applications/%s/%s/nodes", GlobalRuntimeContext.ENV,GlobalRuntimeContext.APPID);

	private ZooKeeper zookeeper = null;
	private volatile List<String> nodeIds;

	public int generate(String nodeId) {
		String zkServers = ResourceUtils.getProperty("application.zookeeper.servers");
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
