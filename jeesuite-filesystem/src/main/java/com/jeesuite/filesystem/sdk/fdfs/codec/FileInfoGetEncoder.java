package com.jeesuite.filesystem.sdk.fdfs.codec;

import com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants;
import com.jeesuite.filesystem.sdk.fdfs.FileId;

/**
 * @author siuming
 */
public class FileInfoGetEncoder extends FileIdOperationEncoder {

    /**
     * @param fileId
     */
    public FileInfoGetEncoder(FileId fileId) {
        super(fileId);
    }

    @Override
    protected byte cmd() {
        return FastdfsConstants.Commands.FILE_QUERY;
    }
}
