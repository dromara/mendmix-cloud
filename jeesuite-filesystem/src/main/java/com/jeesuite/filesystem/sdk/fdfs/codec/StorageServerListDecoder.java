/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs.codec;

import com.jeesuite.filesystem.sdk.fdfs.FastdfsException;
import com.jeesuite.filesystem.sdk.fdfs.StorageServer;
import com.jeesuite.filesystem.sdk.fdfs.exchange.Replier;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

import static com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants.*;
import static com.jeesuite.filesystem.sdk.fdfs.FastdfsUtils.readString;

/**
 * 存储服务器信息列表解码器
 *
 * @author liulongbiao
 */
public enum StorageServerListDecoder implements Replier.Decoder<List<StorageServer>> {
    INSTANCE;

    @Override
    public List<StorageServer> decode(ByteBuf in) {
        int size = in.readableBytes();
        if (size < FDFS_STORAGE_LEN) {
            throw new FastdfsException("body length : " + size + " is less than required length " + FDFS_STORAGE_LEN);
        }
        if ((size - FDFS_STORAGE_LEN) % FDFS_HOST_LEN != 0) {
            throw new FastdfsException("body length : " + size + " is invalidate. ");
        }

        int count = (size - FDFS_STORAGE_LEN) / FDFS_HOST_LEN + 1;
        List<StorageServer> results = new ArrayList<StorageServer>(count);

        String group = readString(in, FDFS_GROUP_LEN);
        String mainHost = readString(in, FDFS_HOST_LEN);
        int port = (int) in.readLong();

        results.add(new StorageServer(group, mainHost, port));
        for (int i = 1; i < count; i++) {
            String host = readString(in, FDFS_HOST_LEN);
            results.add(new StorageServer(group, host, port));
        }

        return results;
    }

}
