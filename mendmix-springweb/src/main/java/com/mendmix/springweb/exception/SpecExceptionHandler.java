package com.mendmix.springweb.exception;

import com.mendmix.common.model.WrapperResponse;

public interface SpecExceptionHandler
{
    boolean handle(WrapperResponse<?> resp, Throwable e);
}
