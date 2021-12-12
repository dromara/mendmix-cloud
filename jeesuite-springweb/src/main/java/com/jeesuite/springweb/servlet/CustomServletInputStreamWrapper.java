package com.jeesuite.springweb.servlet;

import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年3月7日
 */
public class CustomServletInputStreamWrapper  extends ServletInputStream {

    private byte[] data;
    private int idx = 0;

    /**
     * Creates a new <code>CumtomServletInputStreamWrapper</code> instance.
     *
     * @param data a <code>byte[]</code> value
     */
    public CustomServletInputStreamWrapper(byte[] data) {
        if (data == null)
            data = new byte[0];
        this.data = data;
    }

    @Override
    public int read() throws IOException {
        if (idx == data.length)
            return -1;
        return data[idx++] & 0xff;
    }

	@Override
	public boolean isFinished() {
		return idx == data.length;
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void setReadListener(ReadListener listener) {
		
	}

}
