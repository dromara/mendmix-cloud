package com.mendmix.springweb.exception;

import com.mendmix.common.model.WrapperResponse;

public interface ExceptionResponseConverter {

	Object convert(WrapperResponse<?> standardResponse);
}
