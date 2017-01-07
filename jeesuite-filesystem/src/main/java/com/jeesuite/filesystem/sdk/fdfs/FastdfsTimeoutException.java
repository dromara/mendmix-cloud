/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs;

/**
 * Fastdfs 超时异常
 *
 * @author liulongbiao
 */
public class FastdfsTimeoutException extends FastdfsException {

    /**
     *
     */
    private static final long serialVersionUID = -5384713284430243210L;

    FastdfsTimeoutException(String message) {
        super(message);
    }

}
