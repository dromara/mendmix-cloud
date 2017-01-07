/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs.codec;

import com.jeesuite.filesystem.sdk.fdfs.FileMetadata;
import com.jeesuite.filesystem.sdk.fdfs.exchange.Replier;
import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;

import static com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants.FDFS_FIELD_SEPERATOR;
import static com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants.FDFS_RECORD_SEPERATOR;
import static com.jeesuite.filesystem.sdk.fdfs.FastdfsUtils.readString;

/**
 * 文件属性解码器
 *
 * @author liulongbiao
 */
public enum FileMetadataDecoder implements Replier.Decoder<FileMetadata> {
    INSTANCE;

    @Override
    public FileMetadata decode(ByteBuf buf) {
        String content = readString(buf);

        Map<String, String> values = new HashMap<>();
        String[] pairs = content.split(FDFS_RECORD_SEPERATOR);
        for (String pair : pairs) {
            String[] kv = pair.split(FDFS_FIELD_SEPERATOR, 2);
            if (kv.length == 2) {
                values.put(kv[0], kv[1]);
            }
        }
        return new FileMetadata(values);
    }

}
