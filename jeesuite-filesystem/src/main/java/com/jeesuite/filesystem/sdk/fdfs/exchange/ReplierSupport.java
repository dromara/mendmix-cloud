/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs.exchange;

import com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants;
import com.jeesuite.filesystem.sdk.fdfs.FastdfsException;
import io.netty.buffer.ByteBuf;

import java.util.concurrent.CompletableFuture;

import static com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants.Commands.RESP;

/**
 * 抽象 Replier 基类
 *
 * @author liulongbiao
 */
public abstract class ReplierSupport<T> implements Replier<T> {

    protected boolean atHead = true;
    protected long length;

    @Override
    public void reply(ByteBuf in, CompletableFuture<T> promise) {
        if (atHead) {
            readHead(in);
        }
        readContent(in, promise);
    }

    private void readHead(ByteBuf in) {
        if (in.readableBytes() < FastdfsConstants.FDFS_HEAD_LEN) {
            return;
        }
        length = in.readLong();
        byte cmd = in.readByte();
        byte errno = in.readByte();
        if (errno != 0) {
            throw new FastdfsException("Fastdfs responsed with an error, errno is " + errno);
        }
        if (cmd != RESP) {
            throw new FastdfsException("Expect response command code error : " + cmd);
        }
        long expectLength = expectLength();
        if (expectLength >= 0 && length != expectLength) {
            throw new FastdfsException("Expect response length : " + expectLength + " , but reply length : " + length);
        }
        atHead = false;
    }

    protected long expectLength() {
        return -1;
    }

    protected abstract void readContent(ByteBuf in, CompletableFuture<T> promise);
}
