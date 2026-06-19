package com.example.test_presentation.bookkeeping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("bookkeepingCacheProperties")
@ConfigurationProperties(prefix = "bookkeeping.cache")
public class BookkeepingCacheProperties {

	private boolean enabled = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
