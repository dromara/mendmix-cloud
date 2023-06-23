/*
 * Copyright 2016-2020 www.jeesuite.com.
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
package com.mendmix.springweb.i18n;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ResourceBundleMessageSource;

import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.spring.InstanceFactory;

public class I18nMessageUtils {

	private static ResourceBundleMessageSource messageSource;

	private static ResourceBundleMessageSource getMessageSource() {
		if(messageSource != null)return messageSource;
		synchronized (I18nMessageUtils.class) {
			messageSource = InstanceFactory.getInstance(ResourceBundleMessageSource.class);
			if(messageSource == null) {
				messageSource = new ResourceBundleMessageSource();
			}
		}
		return messageSource;
	}
	
	public static String getMessage(String code, Object...args) {
		try {			
			Locale locale = CurrentRuntimeContext.getLocale();
			return getMessageSource().getMessage(code, args, locale);
		} catch (NoSuchMessageException e) {
			return null;
		}
	}

		
	public static String getMessage(String code,String defaultMessage, Object...args) {
		try {			
			Locale locale = CurrentRuntimeContext.getLocale();
			return getMessageSource().getMessage(code, args, defaultMessage, locale);
		} catch (NoSuchMessageException e) {
			return StringUtils.isBlank(defaultMessage) ? code : defaultMessage;
		}
	}
	
	
}
