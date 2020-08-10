package io.automatik.engine.api.config;

import java.util.Optional;

public class JobsConfig {

	/**
	 * Determines the type of persistence to be used
	 */
	public Optional<String> type() {
		return Optional.empty();
	}

	/**
	 * Configures file system based jobs service
	 */
	public FileSystemJobsConfig filesystem() {
		return new FileSystemJobsConfig();
	}

	/**
	 * Configures http based jobs service
	 */
	public HttpJobsConfig http() {
		return new HttpJobsConfig();
	}
}
