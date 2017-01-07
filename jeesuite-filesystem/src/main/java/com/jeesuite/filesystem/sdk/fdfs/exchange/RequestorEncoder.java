package com.jeesuite.filesystem.sdk.fdfs.exchange;

import io.netty.buffer.ByteBufAllocator;

import java.util.List;

/**
 * @author siuming
 */
public class RequestorEncoder extends RequestorSupport {

    private final Encoder encoder;

    /**
     * @param encoder
     */
    public RequestorEncoder(Encoder encoder) {
        this.encoder = encoder;
    }

    @Override
    protected List<Object> writeRequests(ByteBufAllocator alloc) {
        return encoder.encode(alloc);
    }

    @Override
    public String toString() {
        return "RequestorEncoder{" +
                "encoder=" + encoder +
                '}';
    }
}
