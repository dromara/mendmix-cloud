/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs;


import java.util.Base64;
import java.util.Objects;

import static com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants.UTF_8;

/**
 * @author siuming
 */
public class FileId {

    private static final char SEPARATER = '/';

    private final String group;
    private final String path;

    /**
     * @param group
     * @param path
     */
    public FileId(String group, String path) {
        this.group = Objects.requireNonNull(group, "group must not be null.");
        this.path = Objects.requireNonNull(path, "path must not be null.");
    }

    /**
     * @return
     */
    public String group() {
        return group;
    }

    /**
     * @return
     */
    public byte[] groupBytes() {
        return group.getBytes(UTF_8);
    }

    /**
     * @return
     */
    public String path() {
        return path;
    }

    /**
     * @return
     */
    public byte[] pathBytes() {
        return path.getBytes(UTF_8);
    }

    /**
     * @return
     */
    public byte[] toBytes() {
        return toString().getBytes(UTF_8);
    }

    /**
     * @return
     */
    @Override
    public String toString() {
        return group + SEPARATER + path;
    }

    /**
     * @return
     */
    public String toBase64String() {
        return Base64.getUrlEncoder().encodeToString(toBytes());
    }

    /**
     * 从全路径构造存储路径
     *
     * @param fullPath
     * @return
     */
    public static FileId fromString(String fullPath) {
        if (fullPath == null || fullPath.length() == 0) {
            throw new IllegalArgumentException("fullPath should not be empty.");
        }
        int idx = fullPath.indexOf(SEPARATER);
        if (idx < 0) {
            throw new IllegalArgumentException("fullPath cannot find path separater.");
        }

        String group = fullPath.substring(0, idx);
        String path = fullPath.substring(idx + 1);
        return new FileId(group, path);
    }

    /**
     * @param base64
     * @return
     */
    public static FileId fromBase64String(String base64) {
        byte[] bytes = Base64.getUrlDecoder().decode(base64.getBytes(UTF_8));
        return fromString(new String(bytes));
    }
}
