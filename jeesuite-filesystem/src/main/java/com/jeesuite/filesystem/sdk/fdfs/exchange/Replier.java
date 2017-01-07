/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs.exchange;

import io.netty.buffer.ByteBuf;

import java.util.concurrent.CompletableFuture;

/**
 * 接收处理
 *
 * @author liulongbiao
 */
public interface Replier<T> {

    /**
     * @param in
     * @param promise
     */
    void reply(ByteBuf in, CompletableFuture<T> promise);

    /**
     * 响应解码器
     *
     * @author liulongbiao
     */
    interface Decoder<T> {

        /**
         * 期待的长度值，小于 0 时不验证
         *
         * @return
         */
        default long expectLength() {
            return -1;
        }

        /**
         * 解码响应
         *
         * @param buf
         * @return
         */
        T decode(ByteBuf buf);
    }

    /**
     * 空响应解码器
     *
     * @author liulongbiao
     */
    enum NOPDecoder implements Decoder<Void> {

        INSTANCE;

        @Override
        public long expectLength() {
            return 0;
        }

        @Override
        public Void decode(ByteBuf buf) {
            return null;
        }

    }
}
