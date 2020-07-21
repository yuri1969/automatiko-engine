
package io.automatik.engine.addons.monitoring.integration;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers.DecisionConstants;
import io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers.LocalDateTimeHandler;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocalDateTimeHandlerTest extends AbstractQuantilesTest<LocalDateTimeHandler> {

	@BeforeEach
	public void setUp() {
		registry = new CollectorRegistry();
		handler = new LocalDateTimeHandler("hello", registry);
	}

	@AfterEach
	public void destroy() {
		registry.clear();
	}

	@Test
	public void givenLocalDateTimeMetricsWhenMetricsAreStoredThenTheQuantilesAreCorrect() {
		// Arrange
		LocalDateTime now = LocalDateTime.now();
		Long expectedValue = now.toInstant(ZoneOffset.UTC).toEpochMilli();
		Double[] quantiles = new Double[] { 0.1, 0.25, 0.5, 0.75, 0.9, 0.99 };

		// Act
		handler.record("decision", ENDPOINT_NAME, now);

		// Assert
		for (Double key : quantiles) {
			assertEquals(expectedValue, getQuantile("decision", ENDPOINT_NAME + DecisionConstants.DECISIONS_NAME_SUFFIX,
					ENDPOINT_NAME, key), 5);
		}
	}
}
