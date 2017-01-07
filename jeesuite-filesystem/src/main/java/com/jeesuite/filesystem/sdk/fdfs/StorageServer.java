/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs;

import java.net.InetSocketAddress;


/**
 * 存储服务器信息
 *
 * @author liulongbiao
 */
public class StorageServer {

    private final String group;
    private final String host;
    private final int port;
    private final byte pathIdx;

    /**
     * @param group
     * @param host
     * @param port
     * @param pathIdx
     */
    public StorageServer(String group, String host, int port, byte pathIdx) {
        this.group = group;
        this.host = host;
        this.port = port;
        this.pathIdx = pathIdx;
    }

    /**
     * @param group
     * @param host
     * @param port
     */
    public StorageServer(String group, String host, int port) {
        this(group, host, port, (byte) 0);
    }

    /**
     * @return
     */
    public String group() {
        return group;
    }

    /**
     * @return
     */
    public String host() {
        return host;
    }

    /**
     * @return
     */
    public int port() {
        return port;
    }

    /**
     * @return
     */
    public byte pathIdx() {
        return pathIdx;
    }

    @Override
    public String toString() {
        return '[' + group + ']' + host + ':' + port + '/' + pathIdx;
    }

    /**
     * 获取通信地址
     *
     * @return
     */
    public InetSocketAddress toInetAddress() {
        return new InetSocketAddress(host, port);
    }
}
