package io.automatik.engine.quarkus;

import io.automatik.engine.api.config.MessagingConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class MessagingBuildTimeConfig extends MessagingConfig {

	/**
	 * Enables cloud event format for messages
	 */
	@ConfigItem
	public boolean asCloudevents;

	@Override
	public boolean asCloudevents() {
		return asCloudevents;
	}

}
