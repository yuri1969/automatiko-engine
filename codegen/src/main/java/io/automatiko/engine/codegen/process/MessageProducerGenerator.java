
package io.automatiko.engine.codegen.process;

import static com.github.javaparser.StaticJavaParser.parse;
import static io.automatiko.engine.codegen.CodeGenConstants.AMQP_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.CAMEL_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.FUNCTION_FLOW_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.HTTP_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.JMS_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.KAFKA_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.MQTT_CONNECTOR;
import static io.automatiko.engine.codegen.CodeGenConstants.OUTGOING_PROP_PREFIX;
import static io.automatiko.engine.codegen.CodegenUtils.interpolateEventTypes;
import static io.automatiko.engine.codegen.CodegenUtils.interpolateTypes;

import java.util.Map.Entry;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatiko.engine.api.Functions;
import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.BodyDeclarationComparator;
import io.automatiko.engine.codegen.CodegenUtils;
import io.automatiko.engine.codegen.GeneratorContext;
import io.automatiko.engine.codegen.ImportsOrganizer;
import io.automatiko.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatiko.engine.services.execution.BaseFunctions;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.compiler.canonical.TriggerMetaData;
import io.automatiko.engine.workflow.process.executable.core.Metadata;

public class MessageProducerGenerator {

    private static final String EVENT_DATA_VAR = "eventData";

    private final String relativePath;

    private GeneratorContext context;

    private WorkflowProcess process;
    private String workflowType;
    private final String packageName;
    private final String resourceClazzName;
    private final String modelClazzName;
    private String processId;
    private final String processName;
    private final String classPrefix;
    private final String messageDataEventClassName;
    private DependencyInjectionAnnotator annotator;

    private TriggerMetaData trigger;

    public MessageProducerGenerator(String workflowType, GeneratorContext context, WorkflowProcess process, String modelfqcn,
            String processfqcn, String messageDataEventClassName, TriggerMetaData trigger) {
        this.workflowType = workflowType;
        this.context = context;
        this.process = process;
        this.trigger = trigger;
        this.packageName = process.getPackageName();
        this.processId = process.getId();
        this.processName = processId.substring(processId.lastIndexOf('.') + 1);
        this.classPrefix = StringUtils.capitalize(processName) + CodegenUtils.version(process.getVersion());
        this.resourceClazzName = classPrefix + "MessageProducer_" + trigger.getOwnerId();
        this.relativePath = packageName.replace(".", "/") + "/" + resourceClazzName + ".java";
        this.messageDataEventClassName = messageDataEventClassName;
        this.modelClazzName = modelfqcn;
    }

    public MessageProducerGenerator withDependencyInjection(DependencyInjectionAnnotator annotator) {
        this.annotator = annotator;
        return this;
    }

    public String className() {
        return resourceClazzName;
    }

    public String generatedFilePath() {
        return relativePath;
    }

    protected boolean useInjection() {
        return this.annotator != null;
    }

    protected void appendConnectorSpecificProperties(String connector) {
        String sanitizedName = CodegenUtils.triggerSanitizedName(trigger, process.getVersion());
        if (connector.equals(MQTT_CONNECTOR)) {

            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".merge", "true");
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".topic",
                    (String) trigger.getContext("topic", trigger.getName()));
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".host", "${mqtt.server:localhost}");
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".port", "${mqtt.port:1883}");
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".client-id",
                    classPrefix + "-producer");

            context.addInstruction(
                    "Properties for MQTT based message event '" + trigger.getDescription() + "'");
            context.addInstruction(
                    "\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                            + ".topic' should be used to configure MQTT topic defaults to '"
                            + trigger.getContext("topic", trigger.getName()) + "'");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".host' should be used to configure MQTT host that defaults to localhost");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".port' should be used to configure MQTT port that defaults to 1883");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".client-id' should be used to configure MQTT client id that defaults to '" + classPrefix
                    + "-producer'");
        } else if (connector.equals(CAMEL_CONNECTOR)) {
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".merge", "true");
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".endpoint-uri",
                    (String) trigger.getContext("url", ""));
            context.setApplicationProperty("quarkus.automatiko.messaging.as-cloudevents", "false");
            context.addInstruction(
                    "Properties for Apache Camel based message event '" + trigger.getDescription() + "'");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".endpoint-uri' should be used to configure Apache Camel location");
        } else if (connector.equals(KAFKA_CONNECTOR)) {

            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".merge", "true");
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".bootstrap.servers",
                    "${kafka.bootstrap.servers:localhost\\\\:9092}");
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".topic",
                    (String) trigger.getContext("topic", trigger.getName()));
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".key.serializer",
                    "org.apache.kafka.common.serialization.StringSerializer");
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".value.serializer",
                    "org.apache.kafka.common.serialization.StringSerializer");
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".group.id",
                    classPrefix + "-consumer");
            context.setApplicationProperty("quarkus.automatiko.messaging.as-cloudevents",
                    isServerlessProcess() ? "true" : "false");
            context.addInstruction(
                    "Properties for Apache Kafka based message event '" + trigger.getDescription() + "'");
            context.addInstruction(
                    "\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                            + ".topic' should be used to configure Kafka topic defaults to '"
                            + trigger.getContext("topic", trigger.getName()) + "'");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".bootstrap.servers' should be used to configure Kafka bootstrap servers host that defaults to localhost:9092");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".key.serializer' should be used to configure key deserializer port that defaults to StringSerializer");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".value.serializer' should be used to configure key deserializer port that defaults to StringSerializer");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".group.id' should be used to configure Kafka group id that defaults to '" + classPrefix
                    + "-consumer'");
        } else if (connector.equals(JMS_CONNECTOR)) {
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".merge", "true");
            context.setApplicationProperty("quarkus.index-dependency.sjms.group-id", "io.smallrye.reactive");
            context.setApplicationProperty("quarkus.index-dependency.sjms.artifact-id", "smallrye-reactive-messaging-jms");

            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".destination", sanitizedName.toUpperCase());
            context.setApplicationProperty("quarkus.automatiko.messaging.as-cloudevents",
                    isServerlessProcess() ? "true" : "false");
            context.addInstruction(
                    "Properties for JMS based message event '" + trigger.getDescription() + "'");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".destination' should be used to configure destination (queue or topic) name defaults to '"
                    + context.getApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".destination")
                            .orElse(sanitizedName.toUpperCase())
                    + "'");
        } else if (connector.equals(AMQP_CONNECTOR)) {
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".merge", "true");
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".address", sanitizedName.toUpperCase());
            context.setApplicationProperty("quarkus.automatiko.messaging.as-cloudevents",
                    isServerlessProcess() ? "true" : "false");
            context.addInstruction(
                    "Properties for AMQP based message event '" + trigger.getDescription() + "'");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".address' should be used to configure address name, defaults to "
                    + context.getApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".address")
                            .orElse(sanitizedName.toUpperCase())
                    + "'");
        } else if (connector.equals(HTTP_CONNECTOR)) {
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".url",
                    (String) trigger.getContext("url", "http://localhost:8080/" + sanitizedName));
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".merge", "true");
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".serializer",
                    "io.quarkus.reactivemessaging.http.runtime.serializers.StringSerializer");
            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".method", "POST");
            context.setApplicationProperty("quarkus.automatiko.messaging.as-cloudevents",
                    isServerlessProcess() ? "true" : "false");
            context.addInstruction(
                    "Properties for HTTP based message event '" + trigger.getDescription() + "'");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".url' should be used to configure location of the service to call via HTTP, defaults to "
                    + context.getApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".url")
                            .orElse((String) trigger.getContext("url", "http://localhost:8080/" + sanitizedName))
                    + "'");
            context.addInstruction("\t'" + OUTGOING_PROP_PREFIX + sanitizedName
                    + ".method' should be used to configure HTTP method of the service to call, defaults to "
                    + context.getApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".method")
                            .orElse("POST")
                    + "'");
        }
    }

    protected String producerTemplate(String connector) {
        if ((connector.equals("unknown") || connector.equals(FUNCTION_FLOW_CONNECTOR))
                && workflowType.equals(Process.FUNCTION_FLOW_TYPE)) {
            return "/class-templates/FunctionFlowMessageProducerTemplate.java";
        } else if (connector.equals(MQTT_CONNECTOR)) {
            return "/class-templates/MQTTMessageProducerTemplate.java";
        } else if (connector.equals(CAMEL_CONNECTOR)) {
            return "/class-templates/CamelMessageProducerTemplate.java";
        } else if (connector.equals(KAFKA_CONNECTOR)) {
            return "/class-templates/KafkaMessageProducerTemplate.java";
        } else if (connector.equals(JMS_CONNECTOR)) {
            return "/class-templates/JMSMessageProducerTemplate.java";
        } else if (connector.equals(AMQP_CONNECTOR)) {
            return "/class-templates/AMQPMessageProducerTemplate.java";
        } else if (connector.equals(HTTP_CONNECTOR)) {
            return "/class-templates/HTTPMessageProducerTemplate.java";
        } else {
            return "/class-templates/MessageProducerTemplate.java";
        }
    }

    public String generate() {

        String sanitizedName = CodegenUtils.triggerSanitizedName(trigger, process.getVersion());
        String connector = CodegenUtils.getConnector(OUTGOING_PROP_PREFIX + sanitizedName + ".connector", context,
                (String) trigger.getContext("connector"));
        if (connector != null) {

            context.setApplicationProperty(OUTGOING_PROP_PREFIX + sanitizedName + ".connector", connector);

            appendConnectorSpecificProperties(connector);
        }

        CompilationUnit clazz = parse(
                this.getClass().getResourceAsStream(producerTemplate(connector)));
        clazz.setPackageDeclaration(process.getPackageName());

        // add functions so they can be easily accessed in message producer classes
        clazz.addImport(new ImportDeclaration(BaseFunctions.class.getCanonicalName(), true, true));
        context.getBuildContext().classThatImplement(Functions.class.getCanonicalName())
                .forEach(c -> clazz.addImport(new ImportDeclaration(c, true, true)));

        ClassOrInterfaceDeclaration template = clazz.findFirst(ClassOrInterfaceDeclaration.class).get();
        template.setName(resourceClazzName);

        template.findAll(ClassOrInterfaceType.class).forEach(cls -> interpolateTypes(cls, trigger.getDataType()));
        template.findAll(ClassOrInterfaceType.class).forEach(cls -> interpolateEventTypes(cls, messageDataEventClassName));
        template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("produce"))
                .forEach(md -> {

                    md.getParameters().stream().filter(p -> p.getNameAsString().equals(EVENT_DATA_VAR))
                            .forEach(p -> p.setType(trigger.getDataType()));

                    if (context.getBuildContext().hasClassAvailable("org.eclipse.microprofile.opentracing.Traced")) {
                        md.addAnnotation("org.eclipse.microprofile.opentracing.Traced");
                    }
                });
        template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("configure"))
                .forEach(md -> md.addAnnotation("javax.annotation.PostConstruct"));
        template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("marshall"))
                .forEach(md -> {
                    md.getParameters().stream().filter(p -> p.getNameAsString().equals(EVENT_DATA_VAR))
                            .forEach(p -> p.setType(trigger.getDataType()));
                    md.findAll(ClassOrInterfaceType.class).forEach(
                            t -> t.setName(t.getNameAsString().replace("$DataEventType$", messageDataEventClassName)));
                });

        template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("convert"))
                .forEach(md -> {
                    md.setType(md.getTypeAsString().replace("$DataType$", trigger.getDataType()));

                    md.findAll(CastExpr.class)
                            .forEach(c -> c.setType(c.getTypeAsString().replace("$DataType$", trigger.getDataType())));
                    md.findAll(ClassOrInterfaceType.class)
                            .forEach(t -> t.setName(t.getNameAsString().replace("$DataType$", trigger.getDataType())));

                });

        // used by MQTT to get topic name based on expression
        String topicExpression = (String) trigger.getContext("topicExpression");
        if (topicExpression != null) {
            template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("topic"))
                    .forEach(md -> {
                        BlockStmt body = new BlockStmt();

                        ClassOrInterfaceType stringType = new ClassOrInterfaceType(null, String.class.getCanonicalName());

                        if (topicExpression.contains("id")) {
                            VariableDeclarationExpr idField = new VariableDeclarationExpr(stringType, "id");
                            body.addStatement(new AssignExpr(idField,
                                    new MethodCallExpr(new NameExpr("pi"), "getId"), AssignExpr.Operator.ASSIGN));
                        }

                        if (topicExpression.contains("businessKey")) {
                            VariableDeclarationExpr businessKeyField = new VariableDeclarationExpr(stringType, "businessKey");
                            body.addStatement(new AssignExpr(businessKeyField,
                                    new MethodCallExpr(new NameExpr("pi"), "getCorrelationKey"), AssignExpr.Operator.ASSIGN));
                        }
                        VariableScope variableScope = (VariableScope) ((io.automatiko.engine.workflow.process.core.WorkflowProcess) process)
                                .getDefaultContext(VariableScope.VARIABLE_SCOPE);

                        for (Variable var : variableScope.getVariables()) {

                            if (topicExpression.contains(var.getSanitizedName())) {
                                ClassOrInterfaceType varType = new ClassOrInterfaceType(null, var.getType().getStringType());
                                VariableDeclarationExpr v = new VariableDeclarationExpr(
                                        varType,
                                        var.getSanitizedName());
                                body.addStatement(new AssignExpr(v,
                                        new CastExpr(varType,
                                                new MethodCallExpr(new MethodCallExpr(new NameExpr("pi"), "getVariables"),
                                                        "get")
                                                                .addArgument(new StringLiteralExpr(var.getName()))),
                                        AssignExpr.Operator.ASSIGN));
                            }
                        }
                        body.addStatement(new ReturnStmt(new NameExpr(topicExpression)));
                        md.setBody(body);
                    });
        }
        // used by AMQP to get address name based on expression
        String addressExpression = (String) trigger.getContext("addressExpression");
        if (addressExpression != null) {
            template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("address"))
                    .forEach(md -> {
                        BlockStmt body = new BlockStmt();

                        ClassOrInterfaceType stringType = new ClassOrInterfaceType(null, String.class.getCanonicalName());

                        if (addressExpression.contains("id")) {
                            VariableDeclarationExpr idField = new VariableDeclarationExpr(stringType, "id");
                            body.addStatement(new AssignExpr(idField,
                                    new MethodCallExpr(new NameExpr("pi"), "getId"), AssignExpr.Operator.ASSIGN));
                        }

                        if (addressExpression.contains("businessKey")) {
                            VariableDeclarationExpr businessKeyField = new VariableDeclarationExpr(stringType, "businessKey");
                            body.addStatement(new AssignExpr(businessKeyField,
                                    new MethodCallExpr(new NameExpr("pi"), "getCorrelationKey"), AssignExpr.Operator.ASSIGN));
                        }
                        VariableScope variableScope = (VariableScope) ((io.automatiko.engine.workflow.process.core.WorkflowProcess) process)
                                .getDefaultContext(VariableScope.VARIABLE_SCOPE);

                        for (Variable var : variableScope.getVariables()) {

                            if (addressExpression.contains(var.getSanitizedName())) {
                                ClassOrInterfaceType varType = new ClassOrInterfaceType(null, var.getType().getStringType());
                                VariableDeclarationExpr v = new VariableDeclarationExpr(
                                        varType,
                                        var.getSanitizedName());
                                body.addStatement(new AssignExpr(v,
                                        new CastExpr(varType,
                                                new MethodCallExpr(new MethodCallExpr(new NameExpr("pi"), "getVariables"),
                                                        "get")
                                                                .addArgument(new StringLiteralExpr(var.getName()))),
                                        AssignExpr.Operator.ASSIGN));
                            }
                        }
                        body.addStatement(new ReturnStmt(new NameExpr(addressExpression)));
                        md.setBody(body);
                    });
        }

        // used by FunctionFlow to set subject (used by reply to)
        String subjectExpression = (String) trigger.getContext("subjectExpression");
        if (subjectExpression != null) {
            template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("subject"))
                    .forEach(md -> {
                        BlockStmt body = new BlockStmt();

                        ClassOrInterfaceType stringType = new ClassOrInterfaceType(null, String.class.getCanonicalName());

                        if (subjectExpression.contains("id")) {
                            VariableDeclarationExpr idField = new VariableDeclarationExpr(stringType, "id");
                            body.addStatement(new AssignExpr(idField,
                                    new MethodCallExpr(new NameExpr("pi"), "getId"), AssignExpr.Operator.ASSIGN));
                        }

                        if (subjectExpression.contains("businessKey")) {
                            VariableDeclarationExpr businessKeyField = new VariableDeclarationExpr(stringType, "businessKey");
                            body.addStatement(new AssignExpr(businessKeyField,
                                    new MethodCallExpr(new NameExpr("pi"), "getCorrelationKey"), AssignExpr.Operator.ASSIGN));
                        }
                        if (subjectExpression.contains("referenceId")) {
                            VariableDeclarationExpr idField = new VariableDeclarationExpr(stringType, "referenceId");
                            body.addStatement(new AssignExpr(idField,
                                    new MethodCallExpr(new NameExpr("pi"), "getReferenceId"), AssignExpr.Operator.ASSIGN));
                        }
                        VariableScope variableScope = (VariableScope) ((io.automatiko.engine.workflow.process.core.WorkflowProcess) process)
                                .getDefaultContext(VariableScope.VARIABLE_SCOPE);

                        for (Variable var : variableScope.getVariables()) {

                            if (subjectExpression.contains(var.getSanitizedName())) {
                                ClassOrInterfaceType varType = new ClassOrInterfaceType(null, var.getType().getStringType());
                                VariableDeclarationExpr v = new VariableDeclarationExpr(
                                        varType,
                                        var.getSanitizedName());
                                body.addStatement(new AssignExpr(v,
                                        new CastExpr(varType,
                                                new MethodCallExpr(new MethodCallExpr(new NameExpr("pi"), "getVariables"),
                                                        "get")
                                                                .addArgument(new StringLiteralExpr(var.getName()))),
                                        AssignExpr.Operator.ASSIGN));
                            }
                        }
                        body.addStatement(new ReturnStmt(new NameExpr(subjectExpression)));
                        md.setBody(body);
                    });
        }
        // Camal or HTTP headers
        template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("headers"))
                .forEach(md -> {
                    StringBuilder allHeaderValues = new StringBuilder();
                    for (Entry<String, Object> entry : trigger.getContext().entrySet()) {

                        if (entry.getKey().startsWith("Camel") || entry.getKey().startsWith("HTTP")) {
                            allHeaderValues.append(entry.getValue().toString()).append(" ");
                        }
                    }
                    String allHeaderValuesStr = allHeaderValues.toString();
                    BlockStmt body = new BlockStmt();

                    ClassOrInterfaceType stringType = new ClassOrInterfaceType(null, String.class.getCanonicalName());

                    if (allHeaderValuesStr.contains("id")) {
                        VariableDeclarationExpr idField = new VariableDeclarationExpr(stringType, "id");
                        body.addStatement(new AssignExpr(idField,
                                new MethodCallExpr(new NameExpr("pi"), "getId"), AssignExpr.Operator.ASSIGN));
                    }

                    if (allHeaderValuesStr.contains("businessKey")) {
                        VariableDeclarationExpr businessKeyField = new VariableDeclarationExpr(stringType, "businessKey");
                        body.addStatement(new AssignExpr(businessKeyField,
                                new MethodCallExpr(new NameExpr("pi"), "getCorrelationKey"), AssignExpr.Operator.ASSIGN));
                    }
                    VariableScope variableScope = (VariableScope) ((io.automatiko.engine.workflow.process.core.WorkflowProcess) process)
                            .getDefaultContext(VariableScope.VARIABLE_SCOPE);

                    for (Variable var : variableScope.getVariables()) {

                        if (allHeaderValuesStr.contains(var.getSanitizedName())) {
                            ClassOrInterfaceType varType = new ClassOrInterfaceType(null, var.getType().getStringType());
                            VariableDeclarationExpr v = new VariableDeclarationExpr(
                                    varType,
                                    var.getSanitizedName());
                            body.addStatement(new AssignExpr(v,
                                    new CastExpr(varType,
                                            new MethodCallExpr(new MethodCallExpr(new NameExpr("pi"), "getVariables"),
                                                    "get")
                                                            .addArgument(new StringLiteralExpr(var.getName()))),
                                    AssignExpr.Operator.ASSIGN));
                        }
                    }

                    for (Entry<String, Object> entry : trigger.getContext().entrySet()) {

                        if (entry.getKey().startsWith("Camel")) {

                            body.addStatement(new MethodCallExpr(new NameExpr("metadata"), "putHeader")
                                    .addArgument(new StringLiteralExpr(entry.getKey()))
                                    .addArgument(new NameExpr(entry.getValue().toString())));
                        } else if (entry.getKey().startsWith("HTTP")) {

                            body.addStatement(new MethodCallExpr(new NameExpr("builder"), "addHeader")
                                    .addArgument(new StringLiteralExpr(entry.getKey().replaceFirst("HTTP", "")))
                                    .addArgument(new NameExpr(entry.getValue().toString())));
                        }

                    }

                    if (!md.getTypeAsString().equalsIgnoreCase("void")) {
                        body.addStatement(new ReturnStmt(new NameExpr("metadata")));
                    }
                    md.setBody(body);
                });

        // JMS properties
        template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("properties"))
                .forEach(md -> {
                    StringBuilder allHeaderValues = new StringBuilder();
                    for (Entry<String, Object> entry : trigger.getContext().entrySet()) {

                        if (entry.getKey().startsWith("JMS")) {
                            allHeaderValues.append(entry.getValue().toString()).append(" ");
                        }
                    }
                    String allHeaderValuesStr = allHeaderValues.toString();
                    BlockStmt body = new BlockStmt();

                    ClassOrInterfaceType stringType = new ClassOrInterfaceType(null, String.class.getCanonicalName());

                    if (allHeaderValuesStr.contains("id")) {
                        VariableDeclarationExpr idField = new VariableDeclarationExpr(stringType, "id");
                        body.addStatement(new AssignExpr(idField,
                                new MethodCallExpr(new NameExpr("pi"), "getId"), AssignExpr.Operator.ASSIGN));
                    }

                    if (allHeaderValuesStr.contains("businessKey")) {
                        VariableDeclarationExpr businessKeyField = new VariableDeclarationExpr(stringType, "businessKey");
                        body.addStatement(new AssignExpr(businessKeyField,
                                new MethodCallExpr(new NameExpr("pi"), "getCorrelationKey"), AssignExpr.Operator.ASSIGN));
                    }
                    VariableScope variableScope = (VariableScope) ((io.automatiko.engine.workflow.process.core.WorkflowProcess) process)
                            .getDefaultContext(VariableScope.VARIABLE_SCOPE);

                    for (Variable var : variableScope.getVariables()) {

                        if (allHeaderValuesStr.contains(var.getSanitizedName())) {
                            ClassOrInterfaceType varType = new ClassOrInterfaceType(null, var.getType().getStringType());
                            VariableDeclarationExpr v = new VariableDeclarationExpr(
                                    varType,
                                    var.getSanitizedName());
                            body.addStatement(new AssignExpr(v,
                                    new CastExpr(varType,
                                            new MethodCallExpr(new MethodCallExpr(new NameExpr("pi"), "getVariables"),
                                                    "get")
                                                            .addArgument(new StringLiteralExpr(var.getName()))),
                                    AssignExpr.Operator.ASSIGN));
                        }
                    }

                    for (Entry<String, Object> entry : trigger.getContext().entrySet()) {

                        if (entry.getKey().startsWith("JMS")) {
                            body.addStatement(new MethodCallExpr(new NameExpr("builder"), "with")
                                    .addArgument(new StringLiteralExpr(entry.getKey().replaceFirst("JMS", "")))
                                    .addArgument(new NameExpr(entry.getValue().toString())));
                        }

                    }

                    body.addStatement(new ReturnStmt(new NameExpr("builder")));
                    md.setBody(body);
                });

        // used by Kafka to get key name based on expression
        String keyExpression = (String) trigger.getContext("keyExpression");
        if (keyExpression != null) {
            template.findAll(MethodDeclaration.class).stream().filter(md -> md.getNameAsString().equals("key"))
                    .forEach(md -> {
                        BlockStmt body = new BlockStmt();

                        ClassOrInterfaceType stringType = new ClassOrInterfaceType(null, String.class.getCanonicalName());

                        if (keyExpression.contains("id")) {
                            VariableDeclarationExpr idField = new VariableDeclarationExpr(stringType, "id");
                            body.addStatement(new AssignExpr(idField,
                                    new MethodCallExpr(new NameExpr("pi"), "getId"), AssignExpr.Operator.ASSIGN));
                        }

                        if (keyExpression.contains("businessKey")) {
                            VariableDeclarationExpr businessKeyField = new VariableDeclarationExpr(stringType, "businessKey");
                            body.addStatement(new AssignExpr(businessKeyField,
                                    new MethodCallExpr(new NameExpr("pi"), "getCorrelationKey"), AssignExpr.Operator.ASSIGN));
                        }
                        VariableScope variableScope = (VariableScope) ((io.automatiko.engine.workflow.process.core.WorkflowProcess) process)
                                .getDefaultContext(VariableScope.VARIABLE_SCOPE);

                        for (Variable var : variableScope.getVariables()) {

                            if (keyExpression.contains(var.getSanitizedName())) {
                                ClassOrInterfaceType varType = new ClassOrInterfaceType(null, var.getType().getStringType());
                                VariableDeclarationExpr v = new VariableDeclarationExpr(
                                        varType,
                                        var.getSanitizedName());
                                body.addStatement(new AssignExpr(v,
                                        new CastExpr(varType,
                                                new MethodCallExpr(new MethodCallExpr(new NameExpr("pi"), "getVariables"),
                                                        "get")
                                                                .addArgument(new StringLiteralExpr(var.getName()))),
                                        AssignExpr.Operator.ASSIGN));
                            }
                        }
                        body.addStatement(new ReturnStmt(new NameExpr(keyExpression)));
                        md.setBody(body);
                    });
        }

        template.findAll(MethodDeclaration.class)
                .forEach(md -> {
                    md.findAll(StringLiteralExpr.class)
                            .forEach(str -> str.setString(str.asString().replace("$Trigger$", trigger.getName())));
                });
        template.findAll(MethodDeclaration.class)
                .forEach(md -> {
                    md.findAll(StringLiteralExpr.class)
                            .forEach(str -> str.setString(str.asString().replace("$TriggerType$",
                                    (String) trigger.getContext(Metadata.TRIGGER_TYPE_ATTR, trigger.getName()))));
                });

        if (useInjection()) {
            annotator.withApplicationComponent(template);

            template.findAll(FieldDeclaration.class, fd -> fd.getVariable(0).getNameAsString().equals("emitter"))
                    .forEach(emitterField -> {
                        annotator.withInjection(emitterField);
                        annotator.withOutgoingMessage(emitterField, sanitizedName);
                    });
            template.findAll(FieldDeclaration.class, fd -> fd.getVariables().get(0).getNameAsString().equals("converter"))
                    .forEach(fd -> {
                        annotator.withInjection(fd);
                        fd.getVariable(0)
                                .setType(fd.getVariable(0).getTypeAsString().replace("$DataType$", trigger.getDataType()));
                    });

            template.findAll(FieldDeclaration.class, fd -> fd.getVariable(0).getNameAsString().equals("useCloudEvents"))
                    .forEach(fd -> annotator.withConfigInjection(fd, "quarkus.automatiko.messaging.as-cloudevents"));

            template.findAll(FieldDeclaration.class, fd -> fd.getVariable(0).getNameAsString().equals("useCloudEventsBinary"))
                    .forEach(fd -> annotator.withConfigInjection(fd, "quarkus.automatiko.messaging.as-cloudevents-binary"));

        }
        // add connector and message name as static fields of the class
        FieldDeclaration connectorField = new FieldDeclaration().setStatic(true).setFinal(true)
                .addVariable(new VariableDeclarator(new ClassOrInterfaceType(null, "String"), "CONNECTOR",
                        new StringLiteralExpr(connector)));
        template.addMember(connectorField);

        FieldDeclaration messageNameField = new FieldDeclaration().setStatic(true).setFinal(true)
                .addVariable(new VariableDeclarator(new ClassOrInterfaceType(null, "String"), "MESSAGE",
                        new StringLiteralExpr(trigger.getName())));
        template.addMember(messageNameField);

        if (workflowType.equals(Process.FUNCTION_FLOW_TYPE)) {
            String destination = (String) trigger.getContext("functionType", sanitizedName);
            String sourcePrefix = process.getPackageName() + "." + processId + "." + sanitizedName;

            template.findAll(StringLiteralExpr.class).forEach(vv -> {
                String s = vv.getValue();
                String interpolated = s.replace("$destination$", destination);
                interpolated = interpolated.replace("$sourcePrefix$", sourcePrefix);
                vv.setString(interpolated);
            });
        }

        template.getMembers().sort(new BodyDeclarationComparator());
        ImportsOrganizer.organize(clazz);
        return clazz.toString();
    }

    private boolean isServerlessProcess() {
        return (boolean) process.getMetaData().getOrDefault("IsServerlessWorkflow", false);
    }

}
