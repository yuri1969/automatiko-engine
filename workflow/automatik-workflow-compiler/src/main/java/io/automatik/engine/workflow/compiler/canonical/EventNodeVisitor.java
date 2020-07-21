
package io.automatik.engine.workflow.compiler.canonical;

import static io.automatik.engine.workflow.process.executable.core.Metadata.EVENT_TYPE;
import static io.automatik.engine.workflow.process.executable.core.Metadata.EVENT_TYPE_MESSAGE;
import static io.automatik.engine.workflow.process.executable.core.Metadata.EVENT_TYPE_SIGNAL;
import static io.automatik.engine.workflow.process.executable.core.Metadata.MESSAGE_TYPE;
import static io.automatik.engine.workflow.process.executable.core.Metadata.TRIGGER_REF;
import static io.automatik.engine.workflow.process.executable.core.Metadata.TRIGGER_TYPE;
import static io.automatik.engine.workflow.process.executable.core.factory.EventNodeFactory.METHOD_EVENT_TYPE;
import static io.automatik.engine.workflow.process.executable.core.factory.EventNodeFactory.METHOD_VARIABLE_NAME;

import java.text.MessageFormat;
import java.util.Map;

import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.process.core.node.EventNode;
import io.automatik.engine.workflow.process.executable.core.factory.EventNodeFactory;

public class EventNodeVisitor extends AbstractNodeVisitor<EventNode> {

	@Override
	protected String getNodeKey() {
		return "eventNode";
	}

	@Override
	public void visitNode(String factoryField, EventNode node, BlockStmt body, VariableScope variableScope,
			ProcessMetaData metadata) {
		body.addStatement(getAssignedFactoryMethod(factoryField, EventNodeFactory.class, getNodeId(node), getNodeKey(),
				new LongLiteralExpr(node.getId()))).addStatement(getNameMethod(node, "Event")).addStatement(
						getFactoryMethod(getNodeId(node), METHOD_EVENT_TYPE, new StringLiteralExpr(node.getType())));

		Variable variable = null;
		if (node.getVariableName() != null) {
			body.addStatement(getFactoryMethod(getNodeId(node), METHOD_VARIABLE_NAME,
					new StringLiteralExpr(node.getVariableName())));
			variable = variableScope.findVariable(node.getVariableName());
		}
		if (EVENT_TYPE_SIGNAL.equals(node.getMetaData(EVENT_TYPE))) {
			metadata.addSignal(node.getType(), variable != null ? variable.getType().getStringType() : null);
		} else if (EVENT_TYPE_MESSAGE.equals(node.getMetaData(EVENT_TYPE))) {
			Map<String, Object> nodeMetaData = node.getMetaData();
			try {
				TriggerMetaData triggerMetaData = new TriggerMetaData((String) nodeMetaData.get(TRIGGER_REF),
						(String) nodeMetaData.get(TRIGGER_TYPE), (String) nodeMetaData.get(MESSAGE_TYPE),
						node.getVariableName(), String.valueOf(node.getId())).validate();
				metadata.addTrigger(triggerMetaData);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(MessageFormat
						.format("Invalid parameters for event node \"{0}\": {1}", node.getName(), e.getMessage()), e);
			}
		}
		visitMetaData(node.getMetaData(), body, getNodeId(node));
		body.addStatement(getDoneMethod(getNodeId(node)));
	}

}
