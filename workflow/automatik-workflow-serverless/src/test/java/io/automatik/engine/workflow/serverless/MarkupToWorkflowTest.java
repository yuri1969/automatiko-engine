package io.automatik.engine.workflow.serverless;

import io.automatik.engine.workflow.serverless.utils.WorkflowTestUtils;
import io.serverlessworkflow.api.Workflow;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MarkupToWorkflowTest {

    @ParameterizedTest
    @ValueSource(strings = {"/examples/applicantrequest.json", "/examples/applicantrequest.yml",
            "/examples/carauctionbids.json", "/examples/carauctionbids.yml",
            "/examples/creditcheck.json", "/examples/creditcheck.yml",
            "/examples/eventbasedgreeting.json", "/examples/eventbasedgreeting.yml",
            "/examples/finalizecollegeapplication.json", "/examples/finalizecollegeapplication.yml",
            "/examples/greeting.json", "/examples/greeting.yml",
            "/examples/helloworld.json", "/examples/helloworld.yml",
            "/examples/jobmonitoring.json", "/examples/jobmonitoring.yml",
            "/examples/monitorpatient.json", "/examples/monitorpatient.yml",
            "/examples/parallel.json", "/examples/parallel.yml",
            "/examples/provisionorder.json", "/examples/provisionorder.yml",
            "/examples/sendcloudevent.json", "/examples/sendcloudevent.yml",
            "/examples/solvemathproblems.json", "/examples/solvemathproblems.yml",
            "/examples/foreachstatewithactions.json", "/examples/foreachstatewithactions.yml",
            "/examples/periodicinboxcheck.json", "/examples/periodicinboxcheck.yml",
            "/examples/vetappointmentservice.json", "/examples/vetappointmentservice.yml",
            "/examples/eventbasedtransition.json", "/examples/eventbasedtransition.yml"
    })
    public void testSpecExamplesParsing(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());
        assertTrue(workflow.getStates().size() > 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/features/applicantrequest.json", "/features/applicantrequest.yml"})
    public void testSpecFeatureFunctionRef(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());
        assertTrue(workflow.getStates().size() > 0);

        assertNotNull(workflow.getFunctions());
        assertTrue(workflow.getFunctions().getFunctionDefs().size() == 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/features/vetappointment.json", "/features/vetappointment.yml"})
    public void testSpecFeatureEventRef(String workflowLocation) {
        Workflow workflow = Workflow.fromSource(WorkflowTestUtils.readWorkflowFile(workflowLocation));

        assertNotNull(workflow);
        assertNotNull(workflow.getId());
        assertNotNull(workflow.getName());
        assertNotNull(workflow.getStates());
        assertTrue(workflow.getStates().size() > 0);

        assertNotNull(workflow.getEvents());
        assertTrue(workflow.getEvents().getEventDefs().size() == 2);
    }
}