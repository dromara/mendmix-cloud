package org.dromara.mendmix.springweb.exception;

import org.dromara.mendmix.common.model.WrapperResponse;

public interface ExceptionResponseConverter {

	Object convert(WrapperResponse<?> standardResponse);
}
