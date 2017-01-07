package com.jeesuite.filesystem.sdk.fdfs;

/**
 * @author siuming
 */
public class FileInfo {

    private long fileSize;
    private long createTime;
    private long crc32;
    private String address;

    /**
     * @param builder
     */
    private FileInfo(Builder builder) {
        this.fileSize = builder.fileSize;
        this.createTime = builder.createTime;
        this.crc32 = builder.crc32;
        this.address = builder.address;
    }

    public long fileSize() {
        return fileSize;
    }

    public long createTime() {
        return createTime;
    }

    public long crc32() {
        return crc32;
    }

    public String address() {
        return address;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "fileSize=" + fileSize +
                ", createTime=" + createTime +
                ", crc32=" + crc32 +
                ", address='" + address + '\'' +
                '}';
    }

    /**
     * @return
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        long fileSize;
        long createTime;
        long crc32;
        String address;

        Builder() {
        }

        public Builder fileSize(long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder createTime(long createTime) {
            this.createTime = createTime;
            return this;
        }

        public Builder crc32(long crc32) {
            this.crc32 = crc32;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public FileInfo build() {
            return new FileInfo(this);
        }
    }
}
