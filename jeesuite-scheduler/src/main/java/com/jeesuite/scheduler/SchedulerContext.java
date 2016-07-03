/**
 * 
 */
package com.jeesuite.scheduler;

import java.net.InetAddress;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;

public class SchedulerContext {
	/**
     * 多节点是否只激活一个节点
     */
    private static boolean singleMode = true;
    
    private static String nodeId;
    
    public static void setSingleMode(boolean singleMode) {
    	SchedulerContext.singleMode = singleMode;
	}

	public static boolean isSingleMode() {
		return singleMode;
	}



	public static String getNodeId() {
		if(nodeId == null){
			try {
				nodeId = InetAddress.getLocalHost().getHostName() + "_" + RandomStringUtils.random(3, true, true).toLowerCase();
			} catch (Exception e) {
				nodeId = UUID.randomUUID().toString();
			}
		}
		return nodeId;
	}
    
    
}
