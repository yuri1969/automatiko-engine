package io.automatik.engine.quarkus;

import java.util.Optional;

import io.automatik.engine.api.config.AutomatikConfig;
import io.automatik.engine.api.config.JobsConfig;
import io.automatik.engine.api.config.MessagingConfig;
import io.automatik.engine.api.config.MetricsConfig;
import io.automatik.engine.api.config.PersistenceConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "automatik", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class AutomatikBuildTimeConfig extends AutomatikConfig {

	/**
	 * Specifies service url that can be used to reach out this service over http
	 */
	@ConfigItem
	public Optional<String> serviceUrl;

	/**
	 * Specifies package name for generated classes
	 */
	@ConfigItem
	public Optional<String> packageName;

	/**
	 * Configures metrics
	 */
	@ConfigItem
	public MetricsBuildTimeConfig metrics;

	/**
	 * Configures messaging for automatik
	 */
	@ConfigItem
	public MessagingBuildTimeConfig messaging;

	/**
	 * Configures persistence
	 */
	@ConfigItem
	public PersistenceBuildTimeConfig persistence;

	/**
	 * Configures jobs
	 */
	@ConfigItem
	public JobsBuildTimeConfig jobs;

	@Override
	public Optional<String> serviceUrl() {
		return serviceUrl;
	}

	@Override
	public Optional<String> packageName() {
		return packageName;
	}

	@Override
	public MetricsConfig metrics() {
		return metrics;
	}

	@Override
	public PersistenceConfig persistence() {
		return persistence;
	}

	@Override
	public MessagingConfig messaging() {
		return messaging;
	}

	@Override
	public JobsConfig jobs() {
		return jobs;
	}

}
