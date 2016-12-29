/**
 * 
 */
package com.jeesuite.common.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月29日
 */
public class HashUtils {

	/**
	 * MurMurHash算法，是非加密HASH算法，性能很高，
	 * 比传统的CRC32,MD5，SHA-1（这两个算法都是加密HASH算法，复杂度本身就很高，带来的性能上的损害也不可避免）
	 * 等HASH算法要快很多，而且据说这个算法的碰撞率很低. http://murmurhash.googlepages.com/
	 */
	public static Long hash(String key) {
        if(key == null)return 0L;
		ByteBuffer buf = ByteBuffer.wrap(key.getBytes());
		int seed = 0x1234ABCD;

		ByteOrder byteOrder = buf.order();
		buf.order(ByteOrder.LITTLE_ENDIAN);

		long m = 0xc6a4a7935bd1e995L;
		int r = 47;

		long h = seed ^ (buf.remaining() * m);

		long k;
		while (buf.remaining() >= 8) {
			k = buf.getLong();

			k *= m;
			k ^= k >>> r;
			k *= m;

			h ^= k;
			h *= m;
		}

		if (buf.remaining() > 0) {
			ByteBuffer finish = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
			finish.put(buf).rewind();
			h ^= finish.getLong();
			h *= m;
		}

		h ^= h >>> r;
		h *= m;
		h ^= h >>> r;

		buf.order(byteOrder);
		return Math.abs(h);
	}
}
