/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.springweb.servlet;

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
