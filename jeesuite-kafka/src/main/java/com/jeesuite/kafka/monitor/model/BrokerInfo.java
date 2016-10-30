/**
 * 
 */
package com.jeesuite.kafka.monitor.model;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年10月28日
 */
public class BrokerInfo {

	private String id;
	private String host;
	private int port;
	private int version;
	
	public BrokerInfo() {}

	public BrokerInfo(String id, String host, int port) {
		super();
		this.id = id;
		this.host = host;
		this.port = port;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}
	
	
	
}
