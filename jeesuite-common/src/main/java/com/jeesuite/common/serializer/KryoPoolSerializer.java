package com.jeesuite.common.serializer;

import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月28日
 */
public class KryoPoolSerializer implements Serializer{

    /**
     * Kryo 的包装
     */
    private static class KryoHolder {
        private Kryo kryo;
        static final int BUFFER_SIZE = 1024;
        private Output output = new Output(BUFFER_SIZE, -1);     //reuse
        private Input input = new Input();

        KryoHolder(Kryo kryo) {
            this.kryo = kryo;
        }

    }


    interface KryoPool {

        /**
         * get o kryo object
         * @return KryoHolder instance
         */
        KryoHolder get();

        /**
         * return object
         * @param kryo holder
         */
        void offer(KryoHolder kryo);
    }



    /**
     * 由于kryo创建的代价相对较高 ，这里使用空间换时间
     * 对KryoHolder对象进行重用
     */
    public static class KryoPoolImpl implements KryoPool {
        /**
         * default is 1500
         * online server limit 3K
         */

        /**
         * thread safe list
         */
        private final Deque<KryoHolder> kryoHolderDeque=new ConcurrentLinkedDeque<KryoHolder>();

        private KryoPoolImpl() {

        }

        /**
         * @return KryoPool instance
         */
        public static KryoPool getInstance() {
            return Singleton.pool;
        }

        /**
         * get o KryoHolder object
         *
         * @return KryoHolder instance
         */
        @Override
        public KryoHolder get() {
            KryoHolder kryoHolder = kryoHolderDeque.pollFirst();       // Retrieves and removes the head of the queue represented by this table
            return kryoHolder == null ? creatInstnce() : kryoHolder;
        }

        /**
         * create a new kryo object to application use
         * @return KryoHolder instance
         */
        public KryoHolder creatInstnce() {
            Kryo kryo = new Kryo();
            kryo.setReferences(false);//
            return new KryoHolder(kryo);
        }

        /**
         * return object
         * Inserts the specified element at the tail of this queue.
         *
         * @param kryoHolder ...
         */
        @Override
        public void offer(KryoHolder kryoHolder) {
            kryoHolderDeque.addLast(kryoHolder);
        }

        /**
         * creat a Singleton
         */
        private static class Singleton {
            private static final KryoPool pool = new KryoPoolImpl();
        }
    }

    @Override
    public String name() {
        return "kryo_pool_ser";
    }

    /**
     * Serialize object
     * @param obj what to serialize
     * @return return serialize data
     */
    @Override
    public byte[] serialize(Object obj) throws IOException {
		KryoHolder kryoHolder = null;
		if (obj == null)
			throw new RuntimeException("obj can not be null");
		try {
			kryoHolder = KryoPoolImpl.getInstance().get();
			kryoHolder.output.clear(); // clear Output -->每次调用的时候 重置
			kryoHolder.kryo.writeClassAndObject(kryoHolder.output, obj);
			return kryoHolder.output.toBytes();// 无法避免拷贝 ~~~
		} catch (RuntimeException e) {
			throw new RuntimeException(e);
		} finally {
			if (kryoHolder != null) {
				KryoPoolImpl.getInstance().offer(kryoHolder);
			}
			// obj = null; //GC
		}
    }

    /**
     * Deserialize data
     * @param bytes what to deserialize
     * @return object
     */
    @Override
    public Object deserialize(byte[] bytes) throws IOException {
        KryoHolder kryoHolder = null;
        if (bytes == null) throw new RuntimeException("bytes can not be null");
        try {
            kryoHolder = KryoPoolImpl.getInstance().get();
            kryoHolder.input.setBuffer(bytes, 0, bytes.length);//call it ,and then use input object  ,discard any array
            return kryoHolder.kryo.readClassAndObject(kryoHolder.input);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
		} finally {
			if (kryoHolder != null) {
				KryoPoolImpl.getInstance().offer(kryoHolder);
			}
			// bytes = null; // for gc
		}
    }
}
