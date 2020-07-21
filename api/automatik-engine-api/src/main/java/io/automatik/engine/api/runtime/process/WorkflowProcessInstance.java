
package io.automatik.engine.api.runtime.process;

import java.util.Collection;
import java.util.Date;

import io.automatik.engine.api.workflow.flexible.AdHocFragment;
import io.automatik.engine.api.workflow.flexible.Milestone;

/**
 * A workflow process instance represents one specific instance of a workflow
 * process that is currently executing. It is an extension of a
 * <code>ProcessInstance</code> and contains all runtime state related to the
 * execution of workflow processes.
 *
 * @see io.automatik.engine.api.runtime.process.ProcessInstance
 */
public interface WorkflowProcessInstance extends ProcessInstance, NodeInstanceContainer {

	/**
	 * Returns the value of the variable with the given name. Note that only
	 * variables in the process-level scope will be searched. Returns
	 * <code>null</code> if the value of the variable is null or if the variable
	 * cannot be found.
	 *
	 * @param name the name of the variable
	 * @return the value of the variable, or <code>null</code> if it cannot be found
	 */
	Object getVariable(String name);

	/**
	 * Sets process variable with given value under given name
	 * 
	 * @param name  name of the variable
	 * @param value value of the variable
	 */
	void setVariable(String name, Object value);

	/**
	 * Returns start date of this process instance
	 * 
	 * @return actual start date
	 */
	Date getStartDate();

	/**
	 * Returns end date (either completed or aborted) of this process instance
	 * 
	 * @return actual end date
	 */
	Date getEndDate();

	/**
	 * Returns node definition id associated with node instance that failed in case
	 * this process instance is in an error
	 * 
	 * @return node definition id of the failed node instance
	 */
	String getNodeIdInError();

	/**
	 * Returns error message associated with this process instance in case it is in
	 * an error state. It will consists of
	 * <ul>
	 * <li>unique error id (uuid)</li>
	 * <li>fully qualified class name of the root cause</li>
	 * <li>error message of the root cause</li>
	 * </ul>
	 * 
	 * @return error message
	 */
	String getErrorMessage();

	/**
	 * Returns optional correlation key assigned to process instance
	 * 
	 * @return correlation key if present otherwise null
	 */
	String getCorrelationKey();

	/**
	 * Returns the list of Milestones and their status in the current process
	 * instances
	 * 
	 * @return Milestones defined in the process
	 */
	Collection<Milestone> milestones();

	/**
	 * @return AdHocFragments from the process instances
	 */
	Collection<AdHocFragment> adHocFragments();

}
