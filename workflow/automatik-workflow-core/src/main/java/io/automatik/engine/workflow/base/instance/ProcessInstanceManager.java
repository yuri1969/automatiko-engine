
package io.automatik.engine.workflow.base.instance;

import java.util.Collection;

import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.services.correlation.CorrelationKey;

public interface ProcessInstanceManager {

	ProcessInstance getProcessInstance(String id);

	ProcessInstance getProcessInstance(String id, boolean readOnly);

	ProcessInstance getProcessInstance(CorrelationKey correlationKey);

	Collection<ProcessInstance> getProcessInstances();

	void addProcessInstance(ProcessInstance processInstance, CorrelationKey correlationKey);

	void internalAddProcessInstance(ProcessInstance processInstance);

	void removeProcessInstance(ProcessInstance processInstance);

	void internalRemoveProcessInstance(ProcessInstance processInstance);

	void clearProcessInstances();

	void clearProcessInstancesState();

}
