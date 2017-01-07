/**
 *
 */
package com.jeesuite.filesystem.sdk.fdfs;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants.FDFS_FIELD_SEPERATOR;
import static com.jeesuite.filesystem.sdk.fdfs.FastdfsConstants.FDFS_RECORD_SEPERATOR;


public class FileMetadata {

    public static byte OVERWRITE_FLAG = 'O';
    public static byte MERGE_FLAG = 'M';

    private final Map<String, String> values;

    /**
     * @param builder
     */
    private FileMetadata(Builder builder) {
        this.values = builder.values;
    }

    /**
     * @param values
     */
    public FileMetadata(Map<String, String> values) {
        this.values = new HashMap<>(values);
    }

    /**
     * @return
     */
    public Map<String, String> values() {
        return Collections.unmodifiableMap(values);
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "values=" + values +
                '}';
    }

    /**
     * @param charset
     * @return
     */
    public byte[] toBytes(Charset charset) {

        if (values.isEmpty()) {
            return new byte[0];
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                sb.append(FDFS_RECORD_SEPERATOR);
            }
            sb.append(entry.getKey());
            sb.append(FDFS_FIELD_SEPERATOR);
            sb.append(entry.getValue());
            first = false;
        }
        return sb.toString().getBytes(charset);
    }

    /**
     * @return
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        Map<String, String> values = new HashMap<>();

        Builder() {
        }

        public Builder put(String name, String value) {
            this.values.put(name, value);
            return this;
        }

        public Builder putAll(Map<String, String> values) {
            this.values.putAll(values);
            return this;
        }

        public FileMetadata build() {
            return new FileMetadata(this);
        }
    }

}
