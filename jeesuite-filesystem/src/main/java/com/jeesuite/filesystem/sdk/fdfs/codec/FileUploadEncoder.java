/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs.codec;

import com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants;
import com.jeesuite.filesystem.sdk.fdfs.FastdfsUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.io.File;

import static com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants.*;
import static com.jeesuite.filesystem.sdk.fdfs.FastdfsUtils.writeFixLength;

/**
 * 上传请求
 *
 * @author liulongbiao
 */
public class FileUploadEncoder extends FileOperationEncoder {

    private final String ext;
    private final byte pathIdx;

    public FileUploadEncoder(File file, byte pathIdx) {
        super(file);
        this.ext = FastdfsUtils.getFileExt(file.getName(), "tmp");
        this.pathIdx = pathIdx;
    }

    public FileUploadEncoder(Object content, String filename, long size, byte pathIdx) {
        super(content, size);
        this.ext = FastdfsUtils.getFileExt(filename, "tmp");
        this.pathIdx = pathIdx;
    }

    @Override
    protected ByteBuf metadata(ByteBufAllocator alloc) {
        int metaLen = FDFS_STORE_PATH_INDEX_LEN + FDFS_PROTO_PKG_LEN_SIZE + FDFS_FILE_EXT_LEN;
        ByteBuf buf = alloc.buffer(metaLen);
        buf.writeByte(pathIdx);
        buf.writeLong(size());
        writeFixLength(buf, ext, FDFS_FILE_EXT_LEN);
        return buf;
    }

    protected byte cmd() {
        return FastdfsConstants.Commands.FILE_UPLOAD;
    }

}
