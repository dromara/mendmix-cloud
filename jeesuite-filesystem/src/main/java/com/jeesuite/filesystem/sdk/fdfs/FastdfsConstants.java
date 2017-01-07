/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs;

import java.nio.charset.Charset;

/**
 * 常量定义
 *
 * @author liulongbiao
 */
public interface FastdfsConstants {

    Charset UTF_8 = Charset.forName("utf-8");

    int FDFS_HEAD_LEN = 10; // 头部长度
    int FDFS_LONG_LEN = 8; // long 所占字节数
    int FDFS_PROTO_PKG_LEN_SIZE = FDFS_LONG_LEN; // 包长字节数
    int FDFS_FILE_EXT_LEN = 6; // 文件扩展名字节数
    int FDFS_GROUP_LEN = 16; // 文件分组字节数
    int FDFS_HOST_LEN = 15; // 主机地址字节数
    int FDFS_PORT_LEN = FDFS_LONG_LEN; // 端口 字节数
    int FDFS_STORE_PATH_INDEX_LEN = 1; // pathIdx 字节数
    int FDFS_STORAGE_LEN = FDFS_GROUP_LEN + FDFS_HOST_LEN + FDFS_PORT_LEN; // 存储服务器信息字节数
    int FDFS_STORAGE_STORE_LEN = FDFS_STORAGE_LEN + FDFS_STORE_PATH_INDEX_LEN; // 存储服务器信息及存储路径索引字节数

    @Deprecated
    byte METADATA_OVERWRITE = 'O';
    @Deprecated
    byte METADATA_MERGE = 'M';

    String FDFS_RECORD_SEPERATOR = "\u0001";
    String FDFS_FIELD_SEPERATOR = "\u0002";
    byte ERRNO_OK = 0;

    /**
     * Fastdfs 用到的命令代码
     *
     * @author liulongbiao
     */
    interface Commands {

        byte QUIT = 82;
        byte RESP = 100;
        byte ACTIVE_TEST = 111;

        byte SERVER_LIST_GROUP = 91;
        byte SERVER_LIST_STORAGE = 92;
        byte SERVER_DELETE_STORAGE = 93;

        byte SERVICE_QUERY_STORE_WITHOUT_GROUP_ONE = 101;
        byte SERVICE_QUERY_FETCH_ONE = 102;
        byte SERVICE_QUERY_UPDATE = 103;
        byte SERVICE_QUERY_STORE_WITH_GROUP_ONE = 104;
        byte SERVICE_QUERY_FETCH_ALL = 105;
        byte SERVICE_QUERY_STORE_WITHOUT_GROUP_ALL = 106;
        byte SERVICE_QUERY_STORE_WITH_GROUP_ALL = 107;

        byte FILE_UPLOAD = 11;
        byte FILE_DELETE = 12;
        byte FILE_DOWNLOAD = 14;
        byte FILE_QUERY = 22;
        byte FILE_APPEND = 24; // append file
        byte FILE_MODIFY = 34; // modify appender file
        byte FILE_TRUNCATE = 36; // truncate appender file

        byte SLAVE_FILE_UPLOAD = 21;
        byte APPENDER_FILE_UPLOAD = 23; // stream appender file

        byte METADATA_SET = 13;
        byte METADATA_GET = 15;
    }
}
