/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs.codec;

import com.jeesuite.filesystem.sdk.fdfs.exchange.Requestor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.stream.ChunkedNioStream;
import io.netty.handler.stream.ChunkedStream;

import java.io.File;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.LinkedList;
import java.util.List;

import static com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants.ERRNO_OK;
import static com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants.FDFS_HEAD_LEN;

/**
 * 抽象文件请求
 *
 * @author liulongbiao
 */
abstract class FileOperationEncoder implements Requestor.Encoder {

    private final Object content;
    private final long size;

    FileOperationEncoder(File file) {
        long length = file.length();
        this.content = new DefaultFileRegion(file, 0, length);
        this.size = length;
    }

    FileOperationEncoder(Object content, long size) {
        this.content = toContent(content);
        this.size = size;
    }

    private static Object toContent(Object content) {

        if (content instanceof File) {
            File file = (File) content;
            return new DefaultFileRegion(file, 0, file.length());
        }

        if (content instanceof InputStream) {
            return new ChunkedStream((InputStream) content);
        }

        if (content instanceof ReadableByteChannel) {
            return new ChunkedNioStream((ReadableByteChannel) content);
        }

        if (content instanceof byte[]) {
            return Unpooled.wrappedBuffer((byte[]) content);
        }

        throw new IllegalArgumentException("unknown content type : " + content.getClass().getName());
    }

    @Override
    public List<Object> encode(ByteBufAllocator alloc) {
        ByteBuf meta = metadata(alloc);

        ByteBuf head = alloc.buffer(FDFS_HEAD_LEN);
        head.writeLong(meta.readableBytes() + size);
        head.writeByte(cmd());
        head.writeByte(ERRNO_OK);

        CompositeByteBuf cbb = alloc.compositeBuffer();
        cbb.addComponents(head, meta);
        cbb.writerIndex(head.readableBytes() + meta.readableBytes());

        List<Object> requests = new LinkedList<>();
        requests.add(cbb);
        requests.add(content);
        return requests;
    }

    /**
     * @return
     */
    protected Object content() {
        return content;
    }

    /**
     * @return
     */
    protected long size() {
        return size;
    }

    /**
     * @return
     */
    protected abstract byte cmd();

    /**
     * @param alloc
     * @return
     */
    protected abstract ByteBuf metadata(ByteBufAllocator alloc);
}
