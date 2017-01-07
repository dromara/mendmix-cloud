/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Fastdfs 连接池组
 *
 * @author liulongbiao
 */
final class FastdfsPoolGroup extends AbstractChannelPoolMap<InetSocketAddress, FastdfsPool> {

    private static final Logger LOG = LoggerFactory.getLogger(FastdfsPoolGroup.class);

    private final EventLoopGroup loopGroup;

    private final long connectTimeout;
    private final long readTimeout;
    private final long idleTimeout;
    private final int maxConnPerHost;

    FastdfsPoolGroup(EventLoopGroup loopGroup,
                     long connectTimeout,
                     long readTimeout,
                     long idleTimeout,
                     int maxConnPerHost) {
        this.loopGroup = loopGroup;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.idleTimeout = idleTimeout;
        this.maxConnPerHost = maxConnPerHost;
    }

    @Override
    protected FastdfsPool newPool(InetSocketAddress addr) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("channel pool created : {}", addr);
        }

        Bootstrap bootstrap = new Bootstrap().channel(NioSocketChannel.class).group(loopGroup);
        bootstrap.remoteAddress(addr);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        return new FastdfsPool(
                bootstrap,
                readTimeout,
                idleTimeout,
                maxConnPerHost
        );
    }
}
