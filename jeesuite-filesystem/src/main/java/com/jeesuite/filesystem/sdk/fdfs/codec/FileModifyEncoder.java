/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs.codec;

import com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants;
import com.jeesuite.filesystem.sdk.fdfs.FileId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.io.File;

/**
 * 修改文件请求
 *
 * @author liulongbiao
 */
public class FileModifyEncoder extends FileOperationEncoder {

    private final FileId fileId;
    private final long offset;

    public FileModifyEncoder(FileId fileId, File file, long offset) {
        super(file);
        this.fileId = fileId;
        this.offset = offset;
    }

    public FileModifyEncoder(FileId fileId, Object content, long size, long offset) {
        super(content, size);
        this.fileId = fileId;
        this.offset = offset;
    }

    @Override
    protected byte cmd() {
        return FastdfsConstants.Commands.FILE_MODIFY;
    }

    @Override
    protected ByteBuf metadata(ByteBufAllocator alloc) {
        byte[] pathBytes = fileId.pathBytes();
        int metaLen = 3 * FastdfsConstants.FDFS_PROTO_PKG_LEN_SIZE + pathBytes.length;
        ByteBuf buf = alloc.buffer(metaLen);
        buf.writeLong(pathBytes.length);
        buf.writeLong(offset);
        buf.writeLong(size());
        buf.writeBytes(pathBytes);
        return buf;
    }
}
