package io.automatiko.engine.workflow.builder;

import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.executable.core.Metadata;

/**
 * Builder responsible for building an end node that sends a message
 */
public class SendMessageNodeBuilder extends AbstractNodeBuilder {

    private ActionNode node;

    public SendMessageNodeBuilder(String name, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new ActionNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));

        this.node.setMetaData(Metadata.TRIGGER_REF, name);
        this.node.setMetaData(Metadata.TRIGGER_TYPE, "ProduceMessage");
        this.node.setMetaData("functionFlowContinue", "true");

        workflowBuilder.get().addNode(node);

        contect();
    }

    /**
     * Specifies the type of the message content. If not given it is taken from the data being mapped as the content.
     * 
     * @param type class of the message payload
     * @return the builder
     */
    public SendMessageNodeBuilder type(Class<?> type) {
        node.setMetaData(Metadata.MESSAGE_TYPE, type.getCanonicalName());
        return this;
    }

    /**
     * Optional connector name to be used if there are more connectors used in the project.
     * If only one is defined as project dependency it is auto discovered.<br/>
     * Supported connectors are:
     * <ul>
     * <li>kafka</li>
     * <li>mqtt</li>
     * <li>amqp</li>
     * <li>camel</li>
     * <li>http</li>
     * <li>jms</li>
     * </ul>
     * 
     * @param connector one of the supported connectors
     * @return the builder
     */
    public SendMessageNodeBuilder connector(String connector) {
        node.setMetaData("connector", connector);
        return this;
    }

    /**
     * Maps given data object to the payload of the message
     * 
     * @param name name of the data object
     * @return the builder
     */
    public SendMessageNodeBuilder fromDataObject(String name) {
        if (name != null) {

            Variable var = workflowBuilder.get().getVariableScope().findVariable(name);
            if (var == null) {
                throw new IllegalArgumentException("No data object with name '" + name + " found");
            }
            node.setMetaData(Metadata.MAPPING_VARIABLE, name);
            node.setMetaData(Metadata.MESSAGE_TYPE, var.getType().getClassType().getCanonicalName());
        }
        return this;
    }

    /**
     * NOTE: Applies to MQTT connector only<br/>
     * The topic expression to be used while sending message. It is used when the topic needs to be calculated and it is not
     * a constant that comes from the name of the node
     * 
     * @param expression expression to be evaluated to get the topic name
     * @return the builder
     */
    public SendMessageNodeBuilder mqttTopic(String expression) {
        node.setMetaData("topicExpression", expression);
        return this;
    }

    /**
     * The topic to be used while sending message.
     * 
     * @param topic destination topic
     * @return the builder
     */
    public SendMessageNodeBuilder topic(String topic) {
        node.setMetaData("topic", topic);
        return this;
    }

    /**
     * NOTE: Applies to KAFKA connector only<br/>
     * The key expression to be used while sending message. By default the <code>businessKey</code> of the workflow
     * instance is used as Kafka record key, in case it should be something else an expression can be provided
     * 
     * @param expression expression to be evaluated to get the key
     * @return the builder
     */
    public SendMessageNodeBuilder kafkaKey(String expression) {
        node.setMetaData("keyExpression", expression);
        return this;
    }

    /**
     * NOTE: Applies to AMQP connector only<br/>
     * The address expression to be used while sending message. It is used when the address needs to be calculated and it is not
     * a constant that comes from the name of the node
     * 
     * @param expression expression to be evaluated to get the topic name
     * @return the builder
     */
    public SendMessageNodeBuilder amqpAddress(String expression) {
        node.setMetaData("addressExpression", expression);
        return this;
    }

    /**
     * Optional header value that is used by following connectors
     * <ul>
     * <li>jms - header names must start with <code>JMS</code></li>
     * <li>camel - header names must start with <code>Camel</code></li>
     * <li>http - header names must start with <code>HTTM</code></li>
     * </ul>
     * 
     * @param name name of the header
     * @param value value of the header
     * @return the builder
     */
    public SendMessageNodeBuilder header(String name, String value) {
        node.setMetaData(name, name);
        return this;
    }

    @Override
    protected Node getNode() {
        return this.node;
    }
}