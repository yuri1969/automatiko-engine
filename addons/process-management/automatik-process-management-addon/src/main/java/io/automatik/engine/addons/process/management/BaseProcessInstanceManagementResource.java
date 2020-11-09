
package io.automatik.engine.addons.process.management;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.automatik.engine.api.Application;
import io.automatik.engine.api.auth.SecurityPolicy;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessError;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.ProcessInstanceExecutionException;
import io.automatik.engine.api.workflow.WorkItem;
import io.automatik.engine.services.uow.UnitOfWorkExecutor;

public abstract class BaseProcessInstanceManagementResource<T> implements ProcessInstanceManagement<T> {

    private static final String PROCESS_AND_INSTANCE_REQUIRED = "Process id and Process instance id must be given";
    private static final String PROCESS_NOT_FOUND = "Process with id %s not found";
    private static final String PROCESS_INSTANCE_NOT_FOUND = "Process instance with id %s not found";
    private static final String PROCESS_INSTANCE_NOT_IN_ERROR = "Process instance with id %s is not in error state";

    protected Map<String, Process<?>> processData = new LinkedHashMap<String, Process<?>>();

    protected Application application;

    public BaseProcessInstanceManagementResource(Map<String, Process<?>> processData, Application application) {
        this.processData = processData;
        this.application = application;
    }

    public T doGetInstanceInError(String processId, String processInstanceId) {

        return executeOnInstanceInError(processId, processInstanceId, processInstance -> {
            ProcessError error = processInstance.error().get();

            Map<String, String> data = new HashMap<>();
            data.put("id", processInstance.id());
            data.put("failedNodeId", error.failedNodeId());
            data.put("message", error.errorMessage());

            return buildOkResponse(data);
        });
    }

    public T doGetWorkItemsInProcessInstance(String processId, String processInstanceId) {

        return executeOnInstance(processId, processInstanceId, processInstance -> {
            // use special security policy to bypass auth check as this is management
            // operation
            List<WorkItem> workItems = processInstance.workItems(new SecurityPolicy(null) {
            });

            return buildOkResponse(workItems);
        });
    }

    public T doRetriggerInstanceInError(String processId, String processInstanceId) {

        return executeOnInstanceInError(processId, processInstanceId, processInstance -> {
            processInstance.error().get().retrigger();

            if (processInstance.status() == ProcessInstance.STATE_ERROR) {
                throw new ProcessInstanceExecutionException(processInstance.id(),
                        processInstance.error().get().failedNodeId(), processInstance.error().get().errorMessage());
            } else {
                return buildOkResponse(processInstance.variables());
            }
        });
    }

    public T doSkipInstanceInError(String processId, String processInstanceId) {

        return executeOnInstanceInError(processId, processInstanceId, processInstance -> {
            processInstance.error().get().skip();

            if (processInstance.status() == ProcessInstance.STATE_ERROR) {
                throw new ProcessInstanceExecutionException(processInstance.id(),
                        processInstance.error().get().failedNodeId(), processInstance.error().get().errorMessage());
            } else {
                return buildOkResponse(processInstance.variables());
            }
        });
    }

    public T doTriggerNodeInstanceId(String processId, String processInstanceId, String nodeId) {

        return executeOnInstance(processId, processInstanceId, processInstance -> {
            processInstance.triggerNode(nodeId);

            if (processInstance.status() == ProcessInstance.STATE_ERROR) {
                throw new ProcessInstanceExecutionException(processInstance.id(),
                        processInstance.error().get().failedNodeId(), processInstance.error().get().errorMessage());
            } else {
                return buildOkResponse(processInstance.variables());
            }
        });
    }

    public T doRetriggerNodeInstanceId(String processId, String processInstanceId, String nodeInstanceId) {

        return executeOnInstance(processId, processInstanceId, processInstance -> {
            processInstance.retriggerNodeInstance(nodeInstanceId);

            if (processInstance.status() == ProcessInstance.STATE_ERROR) {
                throw new ProcessInstanceExecutionException(processInstance.id(),
                        processInstance.error().get().failedNodeId(), processInstance.error().get().errorMessage());
            } else {
                return buildOkResponse(processInstance.variables());
            }
        });
    }

    public T doCancelNodeInstanceId(String processId, String processInstanceId, String nodeInstanceId) {

        return executeOnInstance(processId, processInstanceId, processInstance -> {
            processInstance.cancelNodeInstance(nodeInstanceId);

            if (processInstance.status() == ProcessInstance.STATE_ERROR) {
                throw new ProcessInstanceExecutionException(processInstance.id(),
                        processInstance.error().get().failedNodeId(), processInstance.error().get().errorMessage());
            } else {
                return buildOkResponse(processInstance.variables());
            }
        });
    }

    public T doCancelProcessInstanceId(String processId, String processInstanceId) {

        return executeOnInstance(processId, processInstanceId, processInstance -> {
            processInstance.abort();

            if (processInstance.status() == ProcessInstance.STATE_ERROR) {
                throw new ProcessInstanceExecutionException(processInstance.id(),
                        processInstance.error().get().failedNodeId(), processInstance.error().get().errorMessage());
            } else {
                return buildOkResponse(processInstance.variables());
            }
        });
    }

    /*
     * Helper methods
     */

    private T executeOnInstanceInError(String processId, String processInstanceId,
            Function<ProcessInstance<?>, T> supplier) {
        if (processId == null || processInstanceId == null) {
            return badRequestResponse(PROCESS_AND_INSTANCE_REQUIRED);
        }

        Process<?> process = processData.get(processId);
        if (process == null) {
            return notFoundResponse(String.format(PROCESS_NOT_FOUND, processId));
        }

        return UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            Optional<? extends ProcessInstance<?>> processInstanceFound = process.instances()
                    .findById(processInstanceId);
            if (processInstanceFound.isPresent()) {
                ProcessInstance<?> processInstance = processInstanceFound.get();

                if (processInstance.error().isPresent()) {
                    return supplier.apply(processInstance);
                } else {
                    return badRequestResponse(String.format(PROCESS_INSTANCE_NOT_IN_ERROR, processInstanceId));
                }
            } else {
                return notFoundResponse(String.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId));
            }
        });
    }

    private T executeOnInstance(String processId, String processInstanceId, Function<ProcessInstance<?>, T> supplier) {
        if (processId == null || processInstanceId == null) {
            return badRequestResponse(PROCESS_AND_INSTANCE_REQUIRED);
        }

        Process<?> process = processData.get(processId);
        if (process == null) {
            return notFoundResponse(String.format(PROCESS_NOT_FOUND, processId));
        }
        return UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            Optional<? extends ProcessInstance<?>> processInstanceFound = process.instances()
                    .findById(processInstanceId);
            if (processInstanceFound.isPresent()) {
                ProcessInstance<?> processInstance = processInstanceFound.get();

                return supplier.apply(processInstance);
            } else {
                return notFoundResponse(String.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId));
            }
        });
    }

    protected abstract <R> T buildOkResponse(R body);

    protected abstract T badRequestResponse(String message);

    protected abstract T notFoundResponse(String message);
}
