package io.automatik.engine.quarkus;

import java.util.Optional;

import io.automatik.engine.api.config.HttpJobsConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class HttpJobsBuildTimeConfig extends HttpJobsConfig {

	/**
	 * File system path to be used as storage location
	 */
	@ConfigItem
	public Optional<String> url;

	@Override
	public String url() {
		return url.orElse(null);
	}
}
