/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs.codec;

import com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants;
import com.jeesuite.filesystem.sdk.fdfs.FileId;
import com.jeesuite.filesystem.sdk.fdfs.exchange.Requestor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;

import java.util.Collections;
import java.util.List;

import static com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants.*;
import static com.jeesuite.filesystem.sdk.fdfs.FastdfsUtils.writeFixLength;

/**
 * 下载请求
 *
 * @author liulongbiao
 */
public class FileDownloadEncoder implements Requestor.Encoder {

    private static final long DEFAULT_OFFSET = 0L;
    private static final long SIZE_UNLIMIT = 0L;

    private final FileId fileId;
    private final long offset;
    private final long size;

    /**
     * @param fileId
     */
    public FileDownloadEncoder(FileId fileId) {
        this(fileId, DEFAULT_OFFSET, SIZE_UNLIMIT);
    }

    /**
     * @param fileId
     * @param offset
     * @param size
     */
    public FileDownloadEncoder(FileId fileId, long offset, long size) {
        this.fileId = fileId;
        this.offset = offset;
        this.size = size;
    }

    @Override
    public List<Object> encode(ByteBufAllocator alloc) {
        byte[] pathBytes = fileId.pathBytes();
        int length = 2 * FDFS_LONG_LEN + FDFS_GROUP_LEN + pathBytes.length;
        byte cmd = FastdfsConstants.Commands.FILE_DOWNLOAD;

        ByteBuf buf = alloc.buffer(length + FDFS_HEAD_LEN);
        buf.writeLong(length);
        buf.writeByte(cmd);
        buf.writeByte(ERRNO_OK);

        buf.writeLong(offset);
        buf.writeLong(size);
        writeFixLength(buf, fileId.group(), FDFS_GROUP_LEN);
        ByteBufUtil.writeUtf8(buf, fileId.path());
        return Collections.singletonList(buf);
    }

}
