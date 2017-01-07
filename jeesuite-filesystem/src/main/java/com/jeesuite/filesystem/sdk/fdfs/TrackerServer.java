package com.jeesuite.filesystem.sdk.fdfs;

import java.net.InetSocketAddress;

/**
 * @author siuming
 */
public class TrackerServer {

    private String host;
    private int port;
    private int weight;

    /**
     * @param host
     * @param port
     */
    public TrackerServer(String host, int port) {
        this(host, port, -1);
    }

    /**
     * @param host
     * @param port
     * @param weight
     */
    public TrackerServer(String host, int port, int weight) {
        this.host = host;
        this.port = port;
        this.weight = weight;
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
    public int weight() {
        return weight;
    }

    /**
     * @return
     */
    public InetSocketAddress toInetAddress() {
        return new InetSocketAddress(host, port);
    }

    @Override
    public String toString() {
        return "TrackerServer{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", weight=" + weight +
                '}';
    }
}
