
package io.automatik.engine.workflow.base.core;

public interface ContextResolver {

	Context resolveContext(String contextId, Object param);

}
