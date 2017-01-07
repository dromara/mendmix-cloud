/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs.codec;

import com.jeesuite.filesystem.sdk.fdfs.FileId;

import static com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants.Commands.SERVICE_QUERY_FETCH_ONE;

/**
 * 获取可下载的存储服务器
 *
 * @author liulongbiao
 */
public class DownloadStorageGetEncoder extends FileIdOperationEncoder {

    public DownloadStorageGetEncoder(FileId fileId) {
        super(fileId);
    }

    @Override
    protected byte cmd() {
        return SERVICE_QUERY_FETCH_ONE;
    }

}
