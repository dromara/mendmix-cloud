package com.jeesuite.logging.integrate;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.AbstractLookup;
import org.apache.logging.log4j.core.lookup.StrLookup;

import com.jeesuite.common.util.ResourceUtils;

@Plugin(name = "spring", category = StrLookup.CATEGORY)
@Order(value=-1)
public class EnvrionmentLookup extends AbstractLookup {

	public EnvrionmentLookup() {
		super();
	}

	@Override
	public String lookup(LogEvent event, String key) {
		return ResourceUtils.getProperty(key);
	}

}
