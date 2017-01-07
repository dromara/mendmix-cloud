package com.jeesuite.filesystem.sdk.fdfs;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author siuming
 */
final class FastdfsPool implements ChannelPool {

    private static final Logger LOG = LoggerFactory.getLogger(FastdfsPoolGroup.class);
    private final ChannelPool channelPool;

    FastdfsPool(Bootstrap bootstrap, long readTimeout, long idleTimeout, int maxConnPerHost) {
        this.channelPool = new FixedChannelPool(
                bootstrap,
                new FastdfsPoolHandler(readTimeout, idleTimeout),
                maxConnPerHost
        );
    }

    public Future<Channel> acquire() {
        return channelPool.acquire();
    }

    public Future<Void> release(Channel channel, Promise<Void> promise) {
        return channelPool.release(channel, promise);
    }

    public Future<Channel> acquire(Promise<Channel> promise) {
        return channelPool.acquire(promise);
    }

    public void close() {
        channelPool.close();
    }

    public Future<Void> release(Channel channel) {
        return channelPool.release(channel);
    }

    private static class FastdfsPoolHandler implements ChannelPoolHandler {
        final long readTimeout;
        final long idleTimeout; // 最大闲置时间(秒)

        FastdfsPoolHandler(long readTimeout, long idleTimeout) {
            this.readTimeout = readTimeout;
            this.idleTimeout = idleTimeout;
        }

        public void channelReleased(Channel channel) throws Exception {
            if (LOG.isDebugEnabled()) {
                LOG.debug("channel released : {}", channel.toString());
            }

            channel.pipeline().get(FastdfsHandler.class).operation(null);
        }

        public void channelAcquired(Channel channel) throws Exception {
            if (LOG.isDebugEnabled()) {
                LOG.debug("channel acquired : {}", channel.toString());
            }

            channel.pipeline().get(FastdfsHandler.class).operation(null);
        }

        public void channelCreated(Channel channel) throws Exception {
            if (LOG.isInfoEnabled()) {
                LOG.info("channel created : {}", channel.toString());
            }

            ChannelPipeline pipeline = channel.pipeline();
            pipeline.addLast(new IdleStateHandler(readTimeout, 0, idleTimeout, TimeUnit.MILLISECONDS));
            pipeline.addLast(new ChunkedWriteHandler()).addLast(new FastdfsHandler());
        }
    }
}
