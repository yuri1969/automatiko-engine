
package io.automatik.engine.workflow.serverless.api.deserializers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import io.automatik.engine.workflow.serverless.api.WorkflowPropertySource;
import io.automatik.engine.workflow.serverless.api.interfaces.Extension;

public class ExtensionDeserializer extends StdDeserializer<Extension> {

	private WorkflowPropertySource context;
	private Map<String, Class<? extends Extension>> extensionsMap = new HashMap<>();
	private static Logger logger = LoggerFactory.getLogger(ExtensionDeserializer.class);

	public ExtensionDeserializer() {
		this(Extension.class);
	}

	public ExtensionDeserializer(Class<?> vc) {
		super(vc);
	}

	public ExtensionDeserializer(WorkflowPropertySource context) {
		this(Extension.class);
		this.context = context;
	}

	public void addExtension(String extensionId, Class<? extends Extension> extensionClass) {
		this.extensionsMap.put(extensionId, extensionClass);
	}

	@Override
	public Extension deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

		ObjectMapper mapper = (ObjectMapper) jp.getCodec();
		JsonNode node = jp.getCodec().readTree(jp);

		String extensionId = node.get("extensionid").asText();

		if (context != null) {
			try {
				String result = context.getPropertySource().getProperty(extensionId);

				if (result != null) {
					extensionId = result;
				}
			} catch (Exception e) {
				logger.info("Exception trying to evaluate property: {}", e.getMessage());
			}
		}

		// based on the name return the specific extension impl
		if (extensionsMap != null && extensionsMap.containsKey(extensionId)) {
			return mapper.treeToValue(node, extensionsMap.get(extensionId));
		} else {
			throw new IllegalArgumentException("Extension handler not registered for: " + extensionId);
		}
	}
}