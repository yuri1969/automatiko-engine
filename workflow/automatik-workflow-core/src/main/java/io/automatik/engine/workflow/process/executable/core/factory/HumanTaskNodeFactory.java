
package io.automatik.engine.workflow.process.executable.core.factory;

import io.automatik.engine.workflow.base.core.Work;
import io.automatik.engine.workflow.base.core.impl.WorkImpl;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.NodeContainer;
import io.automatik.engine.workflow.process.core.node.HumanTaskNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class HumanTaskNodeFactory extends WorkItemNodeFactory {

	public static final String WORK_TASK_NAME = "TaskName";
	public static final String WORK_ACTOR_ID = "ActorId";
	public static final String WORK_GROUP_ID = "GroupId";
	public static final String WORK_PRIORITY = "Priority";
	public static final String WORK_COMMENT = "Comment";
	public static final String WORK_SKIPPABLE = "Skippable";
	public static final String WORK_CONTENT = "Content";

	public HumanTaskNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer,
			long id) {
		super(nodeContainerFactory, nodeContainer, id);
	}

	@Override
	protected Node createNode() {
		return new HumanTaskNode();
	}

	protected HumanTaskNode getHumanTaskNode() {
		return (HumanTaskNode) getNode();
	}

	@Override
	public HumanTaskNodeFactory name(String name) {
		getNode().setName(name);
		return this;
	}

	@Override
	public HumanTaskNodeFactory inMapping(String parameterName, String variableName) {
		super.inMapping(parameterName, variableName);
		return this;
	}

	@Override
	public HumanTaskNodeFactory outMapping(String parameterName, String variableName) {
		super.outMapping(parameterName, variableName);
		return this;
	}

	@Override
	public HumanTaskNodeFactory waitForCompletion(boolean waitForCompletion) {
		super.waitForCompletion(waitForCompletion);
		return this;
	}

	@Override
	public HumanTaskNodeFactory timer(String delay, String period, String dialect, String action) {
		super.timer(delay, period, dialect, action);
		return this;
	}

	@Override
	public HumanTaskNodeFactory workParameter(String name, Object value) {
		super.workParameter(name, value);
		return this;
	}

	public HumanTaskNodeFactory taskName(String taskName) {
		Work work = getHumanTaskNode().getWork();
		if (work == null) {
			work = new WorkImpl();
			getHumanTaskNode().setWork(work);
		}
		work.setParameter(WORK_TASK_NAME, taskName);
		return this;
	}

	public HumanTaskNodeFactory actorId(String actorId) {
		Work work = getHumanTaskNode().getWork();
		if (work == null) {
			work = new WorkImpl();
			getHumanTaskNode().setWork(work);
		}
		work.setParameter(WORK_ACTOR_ID, actorId);
		return this;
	}

	public HumanTaskNodeFactory groupId(String groupId) {
		Work work = getHumanTaskNode().getWork();
		if (work == null) {
			work = new WorkImpl();
			getHumanTaskNode().setWork(work);
		}
		work.setParameter(WORK_GROUP_ID, groupId);
		return this;
	}

	public HumanTaskNodeFactory priority(String priority) {
		Work work = getHumanTaskNode().getWork();
		if (work == null) {
			work = new WorkImpl();
			getHumanTaskNode().setWork(work);
		}
		work.setParameter(WORK_PRIORITY, priority);
		return this;
	}

	public HumanTaskNodeFactory comment(String comment) {
		Work work = getHumanTaskNode().getWork();
		if (work == null) {
			work = new WorkImpl();
			getHumanTaskNode().setWork(work);
		}
		work.setParameter(WORK_COMMENT, comment);
		return this;
	}

	public HumanTaskNodeFactory skippable(boolean skippable) {
		Work work = getHumanTaskNode().getWork();
		if (work == null) {
			work = new WorkImpl();
			getHumanTaskNode().setWork(work);
		}
		work.setParameter(WORK_SKIPPABLE, Boolean.toString(skippable));
		return this;
	}

	public HumanTaskNodeFactory content(String content) {
		Work work = getHumanTaskNode().getWork();
		if (work == null) {
			work = new WorkImpl();
			getHumanTaskNode().setWork(work);
		}
		work.setParameter(WORK_CONTENT, content);
		return this;
	}

	public HumanTaskNodeFactory swimlane(String swimlane) {
		getHumanTaskNode().setSwimlane(swimlane);
		return this;
	}
}
