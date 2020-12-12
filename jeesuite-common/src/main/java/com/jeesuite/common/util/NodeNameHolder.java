/**
 * 
 */
package com.jeesuite.common.util;

import java.net.InetAddress;

import org.apache.commons.lang3.RandomUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月3日
 */
public class NodeNameHolder {

	public static int WORKER_ID = RandomUtils.nextInt(10, 99);
	private static String nodeId;
	
	public static String getNodeId() {
		if(nodeId != null)return nodeId;
		try {
			nodeId = InetAddress.getLocalHost().getHostAddress() + "_" + WORKER_ID;
		} catch (Exception e) {
			nodeId = String.valueOf(WORKER_ID);
		}
		return nodeId;
	}
	
}
