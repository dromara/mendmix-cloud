/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs.codec;

import com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants;
import com.jeesuite.filesystem.sdk.fdfs.FileId;
import com.jeesuite.filesystem.sdk.fdfs.FileMetadata;
import com.jeesuite.filesystem.sdk.fdfs.exchange.Requestor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants.*;
import static com.jeesuite.filesystem.sdk.fdfs.FastdfsUtils.writeFixLength;
import static io.netty.util.CharsetUtil.UTF_8;

/**
 * 设置文件属性请求
 *
 * @author liulongbiao
 */
public class FileMetadataSetEncoder implements Requestor.Encoder {

    private final FileId fileId;
    private final FileMetadata metadata;
    private final byte flag;

    /**
     * @param fileId
     * @param metadata
     * @param flag
     */
    public FileMetadataSetEncoder(FileId fileId, FileMetadata metadata, byte flag) {
        this.fileId = Objects.requireNonNull(fileId);
        this.metadata = metadata;
        this.flag = flag;
    }

    @Override
    public List<Object> encode(ByteBufAllocator alloc) {
        byte[] pathBytes = fileId.pathBytes();
        byte[] metadatas = metadata.toBytes(UTF_8);
        int length = 2 * FDFS_LONG_LEN + 1 + FDFS_GROUP_LEN + pathBytes.length + metadatas.length;
        byte cmd = FastdfsConstants.Commands.METADATA_SET;

        ByteBuf buf = alloc.buffer(length + FDFS_HEAD_LEN);
        buf.writeLong(length);
        buf.writeByte(cmd);
        buf.writeByte(ERRNO_OK);

        buf.writeLong(pathBytes.length);
        buf.writeLong(metadatas.length);
        buf.writeByte(flag);
        writeFixLength(buf, fileId.group(), FDFS_GROUP_LEN);
        buf.writeBytes(pathBytes);
        buf.writeBytes(metadatas);
        return Collections.singletonList(buf);
    }
}
