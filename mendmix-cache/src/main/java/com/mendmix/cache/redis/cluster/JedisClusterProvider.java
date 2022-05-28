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
package com.mendmix.cache.redis.cluster;

import java.util.HashSet;
import java.util.Set;

import com.mendmix.cache.redis.JedisProvider;

import redis.clients.jedis.BinaryJedisCluster;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;


/**
 * 集群 redis服务提供者
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2015年04月23日
 */
public class JedisClusterProvider implements JedisProvider<JedisCluster,BinaryJedisCluster> {
	
	public static final String MODE = "cluster";

	private Integer maxRedirections = 3; //重试3次
	
	
	private JedisCluster jedisCluster;
	private BinaryJedisCluster binaryJedisCluster;
	
	private String groupName;
	
	private boolean tenantModeEnabled; 

	/**
	 * 
	 */
	public JedisClusterProvider(String groupName,JedisPoolConfig jedisPoolConfig, String[] servers, int timeout) {
		this.groupName = groupName;
		Set<HostAndPort> nodes = this.parseHostAndPort(servers);
		jedisCluster = new JedisCluster(nodes, timeout, maxRedirections,jedisPoolConfig);
		binaryJedisCluster = new BinaryJedisCluster(nodes, timeout, maxRedirections,jedisPoolConfig);
	}

	private Set<HostAndPort> parseHostAndPort(String[] servers){
		try {
			Set<HostAndPort> haps = new HashSet<HostAndPort>();
			
			for (String part : servers) {
				String[] ipAndPort = part.split(":");
				HostAndPort hap = new HostAndPort(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
				haps.add(hap);
			}

			return haps;
		}catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	

	@Override
	public JedisCluster get() {
		return jedisCluster;
	}
	
	@Override
	public BinaryJedisCluster getBinary() {
		return binaryJedisCluster;
	}

	@Override
	public void release() {}

	@Override
	public void destroy() throws Exception{
		jedisCluster.close();
	}


	@Override
	public String mode() {
		return MODE;
	}

	@Override
	public String groupName() {
		return groupName;
	}
	
	public void setTenantModeEnabled(boolean tenantModeEnabled) {
		this.tenantModeEnabled = tenantModeEnabled;
	}
	
	@Override
	public boolean tenantMode() {
		return tenantModeEnabled;
	}

}