/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs.exchange;

import io.netty.buffer.ByteBuf;

import java.util.concurrent.CompletableFuture;

/**
 * 默认接收器实现
 *
 * @author liulongbiao
 */
public class ReplierDecoder<T> extends ReplierSupport<T> {

    private Decoder<T> decoder;

    public ReplierDecoder(Decoder<T> decoder) {
        this.decoder = decoder;
    }

    protected long expectLength() {
        return decoder.expectLength();
    }

    protected void readContent(ByteBuf in, CompletableFuture<T> promise) {

        if (in.readableBytes() < length) {
            return;
        }

        ByteBuf buf = in.readSlice((int) length);
        T result = decoder.decode(buf);
        promise.complete(result);
        atHead = true;
    }

    @Override
    public String toString() {
        return "ReplierDecoder{" +
                "decoder=" + decoder +
                '}';
    }
}
