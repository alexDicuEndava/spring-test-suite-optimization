package com.example.test_presentation.bookkeeping.config;

import java.time.Duration;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bookkeeping.demo", name = "startup-delay-enabled", havingValue = "true")
@ConfigurationProperties(prefix = "bookkeeping.demo")
public class TestStartupDelayConfig {

	private Duration startupDelay = Duration.ofMillis(500);

	public Duration getStartupDelay() {
		return startupDelay;
	}

	public void setStartupDelay(Duration startupDelay) {
		this.startupDelay = startupDelay;
	}

	@PostConstruct
	void delayContextStartup() throws InterruptedException {
		Thread.sleep(startupDelay.toMillis());
	}
}
