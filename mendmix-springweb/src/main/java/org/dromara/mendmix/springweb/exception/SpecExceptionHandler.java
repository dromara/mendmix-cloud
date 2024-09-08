package org.dromara.mendmix.springweb.exception;

import org.dromara.mendmix.common.model.WrapperResponse;

public interface SpecExceptionHandler
{
    boolean handle(WrapperResponse<?> resp, Throwable e);
}
