/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;

final class FastdfsHandler extends ByteToMessageDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(FastdfsHandler.class);

    private volatile FastdfsOperation<?> operation;

    void operation(FastdfsOperation<?> operation) {
        this.operation = operation;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        if (null != operation) {
            operation.await(in);
            return;
        }

        if (in.readableBytes() <= 0) {
            return;
        }

        throw new FastdfsDataOverflowException(
                String.format(
                        "fastdfs channel %s remain %s data bytes, but there is not operation await.",
                        ctx.channel(),
                        in.readableBytes()
                )
        );
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        // read idle event.
        if (evt == IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT
                || evt == IdleStateEvent.READER_IDLE_STATE_EVENT) {

            if (null != operation) {
                throw new FastdfsReadTimeoutException(
                        String.format(
                                "execute %s read timeout.",
                                operation
                        )
                );
            }

            return;
        }

        // all idle event.
        if (evt == IdleStateEvent.FIRST_ALL_IDLE_STATE_EVENT
                || evt == IdleStateEvent.ALL_IDLE_STATE_EVENT) {
            throw new FastdfsTimeoutException("fastdfs channel was idle timeout.");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (null == operation) {
            return;
        }

        if (!operation.isDone()) {
            throw new FastdfsException("channel closed.");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();

        Throwable error = translateException(cause);
        if (null != operation) {
            operation.caught(error);
            return;
        }

        // idle timeout.
        if (error instanceof FastdfsTimeoutException) {
            LOG.debug(error.getMessage(), error);
            return;
        }

        LOG.error(error.getMessage(), error);
    }

    private Throwable translateException(Throwable cause) {
        if (cause instanceof FastdfsException) {
            return cause;
        }

        Throwable unwrap = cause;
        for (; ; ) {

            if (unwrap instanceof InvocationTargetException) {
                unwrap = ((InvocationTargetException) unwrap).getTargetException();
                continue;
            }

            if (unwrap instanceof UndeclaredThrowableException) {
                unwrap = ((UndeclaredThrowableException) unwrap).getUndeclaredThrowable();
                continue;
            }

            break;
        }

        return new FastdfsException("fastdfs operation error.", unwrap);
    }
}
