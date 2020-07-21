package io.automatik.engine.workflow.base.core.event;

import io.automatik.engine.api.event.process.ProcessCompletedEvent;
import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.api.runtime.process.ProcessRuntime;

public class ProcessCompletedEventImpl extends ProcessEvent implements ProcessCompletedEvent {

	private static final long serialVersionUID = 510l;

	public ProcessCompletedEventImpl(final ProcessInstance instance, ProcessRuntime runtime) {
		super(instance, runtime);
	}

	public String toString() {
		return "==>[ProcessCompleted(name=" + getProcessInstance().getProcessName() + "; id="
				+ getProcessInstance().getProcessId() + ")]";
	}
}
