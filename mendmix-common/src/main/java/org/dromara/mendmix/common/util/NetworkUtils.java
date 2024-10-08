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
package org.dromara.mendmix.common.util;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.commons.lang3.StringUtils;

/**
 * 
 * <br>
 * Class Name : NetworkUtils
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年4月20日
 */
public class NetworkUtils {

	public static boolean ping(String host, int timeoutMilliseconds) {
		try {
			InetAddress address = InetAddress.getByName(host);
			return address.isReachable(timeoutMilliseconds);
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean telnet(String hostAndPort, int timeoutMilliseconds) {
		String[] parts = StringUtils.splitByWholeSeparator(StringUtils.splitByWholeSeparator(hostAndPort, ",")[0], ":");
		if (parts.length != 2 || !StringUtils.isNumeric(parts[1])) {
			throw new IllegalArgumentException("Argument error, format as [127.0.0.1:3306]");
		}
		return telnet(parts[0], Integer.parseInt(parts[1]), timeoutMilliseconds);
	}

	public static boolean telnet(String host, int port, int timeoutMilliseconds) {
		Socket server = null;
		try {
			server = new Socket();
			InetSocketAddress address = new InetSocketAddress(host, port);
			if (address.isUnresolved()) {
				System.err.println("Couldn't resolve server ["+host+"] as DNS resolution failed");
            }
			server.connect(address, timeoutMilliseconds);
			return true;
		} catch (Exception e) {
			
			return false;
		} finally {
			try {
				server.close();
			} catch (Exception e2) {
			}
		}
	}

	public static boolean isPortFree(int port) {
		try {
			Socket socket = new Socket("localhost", port);
			socket.close();
			return false;
		} catch (ConnectException e) {
			return true;
		} catch (SocketException e) {
			if (e.getMessage().equals("Connection reset by peer")) {
				return true;
			}
			throw new RuntimeException(e);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String[] args) {
		System.out.println(telnet("micro-zookeeper-svc:2181", 2000));
	}

}
