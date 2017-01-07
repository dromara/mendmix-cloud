package com.jeesuite.filesystem.sdk.fdfs;

import com.jeesuite.filesystem.sdk.fdfs.exchange.Replier;
import com.jeesuite.filesystem.sdk.fdfs.exchange.Requestor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import java.util.concurrent.CompletableFuture;

/**
 * @author siuming
 */
final class FastdfsOperation<T> {

    private final Channel channel;
    private final Requestor requestor;
    private final Replier<T> replier;
    private final CompletableFuture<T> promise;

    FastdfsOperation(Channel channel, Requestor requestor, Replier<T> replier, CompletableFuture<T> promise) {
        this.channel = channel;
        this.requestor = requestor;
        this.replier = replier;
        this.promise = promise;
    }

    void execute() {

        channel.pipeline().get(FastdfsHandler.class).operation(this);
        try {

            requestor.request(channel);
        } catch (Exception e) {
            caught(e);
        }
    }

    boolean isDone() {
        return promise.isDone();
    }

    void await(ByteBuf in) {
        try {

            replier.reply(in, promise);
        } catch (Exception e) {
            caught(e);
        }
    }

    void caught(Throwable cause) {
        promise.completeExceptionally(cause);
    }

    @Override
    public String toString() {
        return "FastdfsOperation{" +
                "channel=" + channel +
                ", replier=" + replier +
                ", requestor=" + requestor +
                '}';
    }
}
