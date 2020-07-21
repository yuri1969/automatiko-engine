
package io.automatik.engine.workflow.process.instance.node;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.automatik.engine.api.definition.process.Connection;
import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.workflow.base.core.context.exclusive.ExclusiveGroup;
import io.automatik.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.base.instance.ProcessInstance;
import io.automatik.engine.workflow.base.instance.context.exclusive.ExclusiveGroupInstance;
import io.automatik.engine.workflow.base.instance.impl.ConstraintEvaluator;
import io.automatik.engine.workflow.process.core.node.Split;
import io.automatik.engine.workflow.process.instance.NodeInstanceContainer;
import io.automatik.engine.workflow.process.instance.WorkflowRuntimeException;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceImpl;

/**
 * Runtime counterpart of a split node.
 * 
 */
public class SplitInstance extends NodeInstanceImpl {

	private static final long serialVersionUID = 510l;

	protected Split getSplit() {
		return (Split) getNode();
	}

	public void internalTrigger(final NodeInstance from, String type) {
		if (!io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
			throw new IllegalArgumentException("A Split only accepts default incoming connections!");
		}
		triggerTime = new Date();
		final Split split = getSplit();

		try {
			executeStrategy(split, type);
		} catch (WorkflowRuntimeException wre) {
			throw wre;
		} catch (Exception e) {
			throw new WorkflowRuntimeException(this, getProcessInstance(), "Unable to execute Split: " + e.getMessage(),
					e);
		}
	}

	protected void executeStrategy(Split split, String type) {
		// TODO make different strategies for each type
		switch (split.getType()) {
		case Split.TYPE_AND:
			triggerCompleted(io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, true);
			break;
		case Split.TYPE_XOR:
			List<Connection> outgoing = split.getDefaultOutgoingConnections();
			int priority = Integer.MAX_VALUE;
			Connection selected = null;
			for (final Iterator<Connection> iterator = outgoing.iterator(); iterator.hasNext();) {
				final Connection connection = (Connection) iterator.next();
				ConstraintEvaluator constraint = (ConstraintEvaluator) split.getConstraint(connection);
				if (constraint != null && constraint.getPriority() < priority && !constraint.isDefault()) {
					try {
						if (constraint.evaluate(this, connection, constraint)) {
							selected = connection;
							priority = constraint.getPriority();
						}
					} catch (RuntimeException e) {
						throw new RuntimeException("Exception when trying to evaluate constraint "
								+ constraint.getName() + " in split " + split.getName(), e);
					}
				}
			}
			((NodeInstanceContainer) getNodeInstanceContainer()).removeNodeInstance(this);
			if (selected == null) {
				for (final Iterator<Connection> iterator = outgoing.iterator(); iterator.hasNext();) {
					final Connection connection = (Connection) iterator.next();
					if (split.isDefault(connection)) {
						selected = connection;
						break;
					}
				}
			}
			if (selected == null) {
				throw new IllegalArgumentException(
						"XOR split could not find at least one valid outgoing connection for split "
								+ getSplit().getName());
			}
			if (!hasLoop(selected.getTo(), split)) {
				setLevel(1);
				((NodeInstanceContainer) getNodeInstanceContainer()).setCurrentLevel(1);
			}
			triggerConnection(selected);
			break;
		case Split.TYPE_OR:
			((NodeInstanceContainer) getNodeInstanceContainer()).removeNodeInstance(this);
			outgoing = split.getDefaultOutgoingConnections();
			boolean found = false;
			List<NodeInstanceTrigger> nodeInstances = new ArrayList<NodeInstanceTrigger>();
			List<Connection> outgoingCopy = new ArrayList<Connection>(outgoing);
			while (!outgoingCopy.isEmpty()) {
				priority = Integer.MAX_VALUE;
				Connection selectedConnection = null;
				ConstraintEvaluator selectedConstraint = null;
				for (final Iterator<Connection> iterator = outgoingCopy.iterator(); iterator.hasNext();) {
					final Connection connection = (Connection) iterator.next();
					ConstraintEvaluator constraint = (ConstraintEvaluator) split.getConstraint(connection);

					if (constraint != null && constraint.getPriority() < priority && !constraint.isDefault()) {
						priority = constraint.getPriority();
						selectedConnection = connection;
						selectedConstraint = constraint;
					}
				}
				if (selectedConstraint == null) {
					break;
				}
				if (selectedConstraint.evaluate(this, selectedConnection, selectedConstraint)) {
					nodeInstances.add(new NodeInstanceTrigger(followConnection(selectedConnection),
							selectedConnection.getToType()));
					found = true;
				}
				outgoingCopy.remove(selectedConnection);
			}

			for (NodeInstanceTrigger nodeInstance : nodeInstances) {
				// stop if this process instance has been aborted / completed
				if (getProcessInstance().getState() != ProcessInstance.STATE_ACTIVE) {
					return;
				}
				triggerNodeInstance(nodeInstance.getNodeInstance(), nodeInstance.getToType());
			}
			if (!found) {
				for (final Iterator<Connection> iterator = outgoing.iterator(); iterator.hasNext();) {
					final Connection connection = (Connection) iterator.next();
					ConstraintEvaluator constraint = (ConstraintEvaluator) split.getConstraint(connection);
					if (constraint != null && constraint.isDefault() || split.isDefault(connection)) {
						triggerConnection(connection);
						found = true;
						break;
					}
				}
			}
			if (!found) {
				throw new IllegalArgumentException(
						"OR split could not find at least one valid outgoing connection for split "
								+ getSplit().getName());
			}
			break;
		case Split.TYPE_XAND:
			((io.automatik.engine.workflow.process.instance.NodeInstanceContainer) getNodeInstanceContainer())
					.removeNodeInstance(this);
			Node node = getNode();
			List<Connection> connections = null;
			if (node != null) {
				connections = node.getOutgoingConnections(type);
			}
			if (connections == null || connections.isEmpty()) {
				((io.automatik.engine.workflow.process.instance.NodeInstanceContainer) getNodeInstanceContainer())
						.nodeInstanceCompleted(this, type);
			} else {
				ExclusiveGroupInstance groupInstance = new ExclusiveGroupInstance();
				io.automatik.engine.api.runtime.process.NodeInstanceContainer parent = getNodeInstanceContainer();
				if (parent instanceof ContextInstanceContainer) {
					((ContextInstanceContainer) parent).addContextInstance(ExclusiveGroup.EXCLUSIVE_GROUP,
							groupInstance);
				} else {
					throw new IllegalArgumentException(
							"An Exclusive AND is only possible if the parent is a context instance container");
				}
				Map<io.automatik.engine.workflow.process.instance.NodeInstance, String> nodeInstancesMap = new HashMap<io.automatik.engine.workflow.process.instance.NodeInstance, String>();
				for (Connection connection : connections) {
					nodeInstancesMap.put(followConnection(connection), connection.getToType());
				}
				for (NodeInstance nodeInstance : nodeInstancesMap.keySet()) {
					groupInstance.addNodeInstance(nodeInstance);
				}
				for (Map.Entry<io.automatik.engine.workflow.process.instance.NodeInstance, String> entry : nodeInstancesMap
						.entrySet()) {
					// stop if this process instance has been aborted / completed
					if (getProcessInstance().getState() != ProcessInstance.STATE_ACTIVE) {
						return;
					}
					boolean hidden = false;
					if (getNode().getMetaData().get("hidden") != null) {
						hidden = true;
					}
					InternalProcessRuntime runtime = getProcessInstance().getProcessRuntime();
					if (!hidden) {
						runtime.getProcessEventSupport().fireBeforeNodeLeft(this, runtime);
					}
					((io.automatik.engine.workflow.process.instance.NodeInstance) entry.getKey()).trigger(this,
							entry.getValue());
					if (!hidden) {
						runtime.getProcessEventSupport().fireAfterNodeLeft(this, runtime);
					}
				}
			}
			break;
		default:
			throw new IllegalArgumentException("Illegal split type " + split.getType());
		}
	}

	protected boolean hasLoop(Node startAt, final Node lookFor) {
		Set<Long> vistedNodes = new HashSet<Long>();

		return checkNodes(startAt, lookFor, vistedNodes);

	}

	protected boolean checkNodes(Node currentNode, final Node lookFor, Set<Long> vistedNodes) {
		List<Connection> connections = currentNode
				.getOutgoingConnections(io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);

		for (Connection conn : connections) {
			Node nextNode = conn.getTo();
			if (nextNode == null) {
				continue;
			} else if (vistedNodes.contains(nextNode.getId())) {
				continue;
			} else {
				vistedNodes.add(nextNode.getId());
				if (nextNode.getId() == lookFor.getId()) {
					return true;
				}

				boolean nestedCheck = checkNodes(nextNode, lookFor, vistedNodes);
				if (nestedCheck) {
					return true;
				}

			}
		}

		return false;
	}

}
