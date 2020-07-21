
package io.automatik.engine.codegen.process;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.codegen.GeneratorContext;

public class ReactiveResourceGenerator extends ResourceGenerator {

	private static final String REACTIVE_RESOURCE_TEMPLATE = "/class-templates/ReactiveRestResourceTemplate.java";

	public ReactiveResourceGenerator(GeneratorContext context, WorkflowProcess process, String modelfqcn,
			String processfqcn, String appCanonicalName) {
		super(context, process, modelfqcn, processfqcn, appCanonicalName);
	}

	@Override
	protected String getResourceTemplate() {
		return REACTIVE_RESOURCE_TEMPLATE;
	}
}