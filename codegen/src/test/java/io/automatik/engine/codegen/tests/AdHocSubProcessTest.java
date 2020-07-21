
package io.automatik.engine.codegen.tests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatik.engine.api.Application;
import io.automatik.engine.api.Model;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.WorkItem;
import io.automatik.engine.codegen.AbstractCodegenTest;

import static io.automatik.engine.codegen.tests.ProcessTestUtils.assertState;
import static org.assertj.core.api.Assertions.assertThat;

class AdHocSubProcessTest extends AbstractCodegenTest {

	@Test
	void testActivationAdHoc() throws Exception {
		Application app = generateCodeProcessesOnly("cases/ActivationAdHoc.bpmn");
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("TestCase.ActivationAdHoc");
		Model model = p.createModel();
		Map<String, Object> params = new HashMap<>();
		params.put("favouriteColour", "yellow");
		model.fromMap(params);
		ProcessInstance<?> processInstance = p.createInstance(model);
		assertState(processInstance, ProcessInstance.STATE_PENDING);
		processInstance.start();

		assertState(processInstance, ProcessInstance.STATE_ACTIVE);

		List<WorkItem> workItems = processInstance.workItems();
		assertThat(workItems.size()).isEqualTo(1);
		WorkItem workItem = workItems.get(0);
		params = new HashMap<>();
		params.put("favouriteColour", "blue");
		processInstance.completeWorkItem(workItem.getId(), params);

		assertState(processInstance, ProcessInstance.STATE_COMPLETED);
	}

	@Test
	void testCompletionAdHoc() throws Exception {
		Application app = generateCodeProcessesOnly("cases/CompletionAdHoc.bpmn");
		assertThat(app).isNotNull();

		Process<? extends Model> p = app.processes().processById("TestCase.CompletionAdHoc");
		Model model = p.createModel();
		Map<String, Object> params = new HashMap<>();
		params.put("favouriteColour", "yellow");
		model.fromMap(params);
		ProcessInstance<?> processInstance = p.createInstance(model);
		assertState(processInstance, ProcessInstance.STATE_PENDING);
		processInstance.start();

		assertState(processInstance, ProcessInstance.STATE_ACTIVE);

		List<WorkItem> workItems = processInstance.workItems();
		assertThat(workItems.size()).isEqualTo(1);
		WorkItem workItem = workItems.get(0);
		workItem.getParameters().put("favouriteColour", "green");
		params.put("favouriteColour", "green");
		processInstance.completeWorkItem(workItem.getId(), params);

		assertState(processInstance, ProcessInstance.STATE_COMPLETED);
	}
}
