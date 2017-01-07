/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs;

import com.jeesuite.filesystem.sdk.fdfs.codec.*;
import com.jeesuite.filesystem.sdk.fdfs.exchange.Replier;
import com.jeesuite.filesystem.sdk.fdfs.exchange.StreamReplier;

import java.io.File;
import java.util.concurrent.CompletableFuture;


final class StorageClient {

    private final FastdfsExecutor executor;

    StorageClient(FastdfsExecutor executor) {
        this.executor = executor;
    }

    /**
     * 上传文件
     *
     * @param server 存储服务器信息，应该由 tracker 查询得到
     * @param file   文件
     * @return
     */
    CompletableFuture<FileId> upload(StorageServer server, File file) {
        return executor.execute(
                server.toInetAddress(),
                new FileUploadEncoder(file, server.pathIdx()),
                FileIdDecoder.INSTANCE
        );
    }

    /**
     * 上传文件，其中文件内容字段 content 的支持以下类型：
     * <p>
     * <ul>
     * <li>{@link java.io.File}</li>
     * <li>{@link java.io.InputStream}</li>
     * <li><code>byte[]</code></li>
     * <li>{@link java.nio.channels.ReadableByteChannel}</li>
     * </ul>
     *
     * @param server   存储服务器信息，应该由 tracker 查询得到
     * @param content  上传内容
     * @param filename 扩展名
     * @param size     内容长度
     */
    CompletableFuture<FileId> upload(StorageServer server, Object content, String filename, long size) {
        return executor.execute(
                server.toInetAddress(),
                new FileUploadEncoder(content, filename, size, server.pathIdx()),
                FileIdDecoder.INSTANCE
        );
    }

    /**
     * 上传可追加文件内容
     *
     * @param server 存储服务器信息，应该由 tracker 查询得到
     * @param file   文件
     */
    CompletableFuture<FileId> uploadAppender(StorageServer server, File file) {
        return executor.execute(
                server.toInetAddress(),
                new FileUploadAppenderEncoder(file, server.pathIdx()),
                FileIdDecoder.INSTANCE
        );
    }

    /**
     * 上传可追加文件内容，其中文件内容字段 content 的支持以下类型：
     * <p>
     * <ul>
     * <li>{@link java.io.File}</li>
     * <li>{@link java.io.InputStream}</li>
     * <li><code>byte[]</code></li>
     * <li>{@link java.nio.channels.ReadableByteChannel}</li>
     * </ul>
     *
     * @param server   存储服务器信息，应该由 tracker 查询得到
     * @param content  上传内容
     * @param size     内容长度
     * @param filename 扩展名
     */
    CompletableFuture<FileId> uploadAppender(StorageServer server, Object content, String filename, long size) {
        return executor.execute(
                server.toInetAddress(),
                new FileUploadAppenderEncoder(content, filename, size, server.pathIdx()),
                FileIdDecoder.INSTANCE
        );
    }

    /**
     * @param server
     * @param fileId
     * @param file
     * @return
     */
    CompletableFuture<Void> append(StorageServer server, FileId fileId, File file) {
        return executor.execute(
                server.toInetAddress(),
                new FileAppendEncoder(fileId, file),
                Replier.NOPDecoder.INSTANCE
        );
    }

    /**
     * 追加文件内容
     *
     * @param server 存储服务器信息，应该由 tracker 查询得到
     * @param fileId 服务器存储路径
     * @param bytes  内容字节数组
     * @return
     */
    CompletableFuture<Void> append(StorageServer server, FileId fileId, byte[] bytes) {
        return executor.execute(
                server.toInetAddress(),
                new FileAppendEncoder(fileId, bytes, bytes.length),
                Replier.NOPDecoder.INSTANCE
        );
    }

    /**
     * @param server
     * @param fileId
     * @param content
     * @param size
     * @return
     */
    CompletableFuture<Void> append(StorageServer server, FileId fileId, Object content, long size) {
        return executor.execute(
                server.toInetAddress(),
                new FileAppendEncoder(fileId, content, size),
                Replier.NOPDecoder.INSTANCE
        );
    }

    /**
     * @param server
     * @param fileId
     * @param file
     * @param offset
     * @return
     */
    CompletableFuture<Void> modify(StorageServer server, FileId fileId, File file, long offset) {
        return executor.execute(
                server.toInetAddress(),
                new FileModifyEncoder(fileId, file, offset),
                Replier.NOPDecoder.INSTANCE
        );
    }

    /**
     * 修改文件内容
     *
     * @param server 存储服务器信息，应该由 tracker 查询得到
     * @param fileId 服务器存储路径
     * @param offset 偏移量
     * @param bytes  内容字节数组
     * @return
     */
    CompletableFuture<Void> modify(StorageServer server, FileId fileId, byte[] bytes, long offset) {
        return executor.execute(
                server.toInetAddress(),
                new FileModifyEncoder(fileId, bytes, bytes.length, offset),
                Replier.NOPDecoder.INSTANCE
        );
    }

    /**
     * @param server
     * @param fileId
     * @param content
     * @param size
     * @param offset
     * @return
     */
    CompletableFuture<Void> modify(StorageServer server, FileId fileId, Object content, long size, long offset) {
        return executor.execute(
                server.toInetAddress(),
                new FileModifyEncoder(fileId, content, size, offset),
                Replier.NOPDecoder.INSTANCE
        );
    }

    /**
     * 删除文件
     *
     * @param server 存储服务器信息，应该由 tracker 查询得到
     * @param fileId 服务器存储路径
     */
    CompletableFuture<Void> delete(StorageServer server, FileId fileId) {
        return executor.execute(
                server.toInetAddress(),
                new FileDeleteEncoder(fileId),
                Replier.NOPDecoder.INSTANCE
        );
    }

    /**
     * 截取文件
     *
     * @param server 存储服务器信息，应该由 tracker 查询得到
     * @param fileId 服务器存储路径
     */
    CompletableFuture<Void> truncate(StorageServer server, FileId fileId) {
        return truncate(server, fileId, 0);
    }

    /**
     * 截取文件
     *
     * @param server        存储服务器信息，应该由 tracker 查询得到
     * @param fileId        服务器存储路径
     * @param truncatedSize 截取文件大小
     */
    CompletableFuture<Void> truncate(StorageServer server, FileId fileId, long truncatedSize) {
        return executor.execute(
                server.toInetAddress(),
                new FileTruncateEncoder(fileId, truncatedSize),
                Replier.NOPDecoder.INSTANCE
        );
    }

    /**
     * 下载文件，其输出 output 参数支持以下类型
     * <p>
     * <ul>
     * <li>{@link java.io.OutputStream}</li>
     * <li>{@link java.nio.channels.GatheringByteChannel}</li>
     * </ul>
     *
     * @param server 存储服务器信息，应该由 tracker 查询得到
     * @param fileId 服务器存储路径
     * @param output 输出流
     * @return 下载进度
     */
    CompletableFuture<Void> download(StorageServer server, FileId fileId, Object output) {
        return executor.execute(
                server.toInetAddress(),
                new FileDownloadEncoder(fileId),
                StreamReplier.stream(output)
        );
    }

    /**
     * 下载文件内容
     *
     * @param server 存储服务器信息，应该由 tracker 查询得到
     * @param fileId 服务器存储路径
     * @param offset 字节偏移量
     * @param size   下载字节数
     * @param output 输出流
     * @return 下载进度
     */
    CompletableFuture<Void> download(StorageServer server, FileId fileId, Object output, long offset, long size) {
        return executor.execute(
                server.toInetAddress(),
                new FileDownloadEncoder(fileId, offset, size),
                StreamReplier.stream(output)
        );
    }

    /**
     * 设置文件元数据
     *
     * @param server   存储服务器信息，应该由 tracker 查询得到
     * @param fileId   服务器存储路径
     * @param metadata 元数据
     * @param flag     设置标识
     * @return
     */
    CompletableFuture<Void> setMetadata(StorageServer server, FileId fileId, FileMetadata metadata, byte flag) {
        return executor.execute(
                server.toInetAddress(),
                new FileMetadataSetEncoder(fileId, metadata, flag),
                Replier.NOPDecoder.INSTANCE
        );
    }

    /**
     * 获取文件元数据
     *
     * @param server 存储服务器信息，应该由 tracker 查询得到
     * @param fileId 服务器存储路径
     * @return
     */
    CompletableFuture<FileMetadata> getMetadata(StorageServer server, FileId fileId) {
        return executor.execute(
                server.toInetAddress(),
                new FileMetadataGetEncoder(fileId),
                FileMetadataDecoder.INSTANCE
        );
    }

    /**
     * @param server
     * @param fileId
     * @return
     */
    CompletableFuture<FileInfo> getInfo(StorageServer server, FileId fileId) {
        return executor.execute(
                server.toInetAddress(),
                new FileInfoGetEncoder(fileId),
                FileInfoDecoder.INSTANCE
        );
    }
}
