/**
 * 
 */
package com.jeesuite.common2.sequence;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import com.jeesuite.common.util.NodeNameHolder;
import com.jeesuite.common.util.ResourceUtils;

/**
 * 全局ID生成器 （根据: https://github.com/twitter/snowflake）
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月18日
 */
public class SnowflakeGenerator implements IdGenerator, Watcher {

	private static final String ROOT_PATH = "/applications/%s/nodes";

	private long workerId;
	private long datacenterId;
	private long sequence = 0L;

	private long twepoch = 1288834974657L;

	private long workerIdBits = 5L;
	private long datacenterIdBits = 5L;
	private long maxWorkerId = -1L ^ (-1L << workerIdBits);
	private long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
	private long sequenceBits = 12L;

	private long workerIdShift = sequenceBits;
	private long datacenterIdShift = sequenceBits + workerIdBits;
	private long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
	private long sequenceMask = -1L ^ (-1L << sequenceBits);

	private long lastTimestamp = -1L;

	private ZooKeeper zk;

	/**
	 * 需要zookeeper保存节点信息
	 */
	public SnowflakeGenerator() {
		try {
			String appName = ResourceUtils.getProperty("spring.application.name", ResourceUtils.getProperty("jeesuite.configcenter.appName"));
			Validate.notBlank(appName, "config[spring.application.name] not found");
			String zkServer = ResourceUtils.getAndValidateProperty("zookeeper.servers");
			
			zk = new ZooKeeper(zkServer, 10000, this);
			String path = String.format(ROOT_PATH, appName);
			
			String[] parts = StringUtils.split(path, "/");
			String tmpParent = "";
			Stat stat;
			for (int i = 0; i < parts.length; i++) {
				tmpParent = tmpParent + "/" + parts[i];
				stat = zk.exists(tmpParent, false);
				if (stat == null) {
					zk.create(tmpParent, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				}
			}
			String nodePath = path + "/" + NodeNameHolder.getNodeId();
			zk.create(nodePath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
			int workerId = zk.getChildren(path, false).size();
			if (workerId > maxWorkerId || workerId < 0) {
				throw new IllegalArgumentException(
						String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
			}
			this.workerId = workerId;
		} catch (Exception e) {
			this.workerId = RandomUtils.nextInt(1, 31);
		}
		this.datacenterId = 1;
	}

	public SnowflakeGenerator(long workerId, long datacenterId) {
		// sanity check for workerId
		if (workerId > maxWorkerId || workerId < 0) {
			throw new IllegalArgumentException(
					String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
		}
		if (datacenterId > maxDatacenterId || datacenterId < 0) {
			throw new IllegalArgumentException(
					String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
		}
		this.workerId = workerId;
		this.datacenterId = datacenterId;
	}

	public synchronized long nextId() {
		long timestamp = timeGen();

		if (timestamp < lastTimestamp) {
			throw new RuntimeException(String.format(
					"Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
		}

		if (lastTimestamp == timestamp) {
			sequence = (sequence + 1) & sequenceMask;
			if (sequence == 0) {
				timestamp = tilNextMillis(lastTimestamp);
			}
		} else {
			sequence = 0L;
		}

		lastTimestamp = timestamp;

		return ((timestamp - twepoch) << timestampLeftShift) | (datacenterId << datacenterIdShift)
				| (workerId << workerIdShift) | sequence;
	}

	protected long tilNextMillis(long lastTimestamp) {
		long timestamp = timeGen();
		while (timestamp <= lastTimestamp) {
			timestamp = timeGen();
		}
		return timestamp;
	}

	protected long timeGen() {
		return System.currentTimeMillis();
	}

	@Override
	public void process(WatchedEvent event) {

	}
	
	public void close(){
		try {
			zk.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	} 

	public static void main(String[] args) {
		ResourceUtils.add("spring.application.name", "demo");
		ResourceUtils.add("zookeeper.servers", "127.0.0.1:2181");
		SnowflakeGenerator generator = new SnowflakeGenerator();
		System.out.println(generator.nextId());
	}
}
