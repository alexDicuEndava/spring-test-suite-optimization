package com.example.test_presentation.bookkeeping.config;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "bookkeeping.demo", name = "startup-delay-enabled", havingValue = "true")
@EnableConfigurationProperties(TestStartupDelayConfig.StartupDelayProperties.class)
public class TestStartupDelayConfig {

	@Bean
	static BeanPostProcessor bookkeepingDemoStartupDelayBeanPostProcessor(StartupDelayProperties properties) {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
				if (properties.getStartupDelayBeanNames().contains(beanName)) {
					sleep(properties.delayPerBean());
				}
				return bean;
			}
		};
	}

	private static void sleep(Duration delay) {
		try {
			Thread.sleep(delay.toMillis());
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while applying demo startup delay", exception);
		}
	}

	@ConfigurationProperties(prefix = "bookkeeping.demo")
	public static class StartupDelayProperties {

		private Duration startupDelay = Duration.ofMillis(500);

		private List<String> startupDelayBeanNames = List.of(
				"customerService",
				"invoiceService",
				"customerController",
				"invoiceController");

		public Duration getStartupDelay() {
			return startupDelay;
		}

		public void setStartupDelay(Duration startupDelay) {
			this.startupDelay = startupDelay;
		}

		public List<String> getStartupDelayBeanNames() {
			return startupDelayBeanNames;
		}

		public void setStartupDelayBeanNames(List<String> startupDelayBeanNames) {
			this.startupDelayBeanNames = startupDelayBeanNames;
		}

		Duration delayPerBean() {
			if (startupDelayBeanNames.isEmpty()) {
				return Duration.ZERO;
			}
			long perBeanMillis = Math.ceilDiv(startupDelay.toMillis(), startupDelayBeanNames.size());
			return Duration.ofMillis(perBeanMillis);
		}
	}
}
