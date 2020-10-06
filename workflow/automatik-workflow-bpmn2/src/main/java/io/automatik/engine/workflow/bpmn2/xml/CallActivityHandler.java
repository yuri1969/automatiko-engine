
package io.automatik.engine.workflow.bpmn2.xml;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatik.engine.api.runtime.process.DataTransformer;
import io.automatik.engine.workflow.base.core.impl.DataTransformerRegistry;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatik.engine.workflow.compiler.xml.XmlDumper;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.NodeContainer;
import io.automatik.engine.workflow.process.core.impl.NodeImpl;
import io.automatik.engine.workflow.process.core.node.ForEachNode;
import io.automatik.engine.workflow.process.core.node.SubProcessNode;
import io.automatik.engine.workflow.process.core.node.Transformation;

public class CallActivityHandler extends AbstractNodeHandler {

    private DataTransformerRegistry transformerRegistry = DataTransformerRegistry.get();

    protected Node createNode(Attributes attrs) {
        return new SubProcessNode();
    }

    @SuppressWarnings("unchecked")
    public Class generateNodeFor() {
        return SubProcessNode.class;
    }

    protected void handleNode(final Node node, final Element element, final String uri, final String localName,
            final ExtensibleXmlParser parser) throws SAXException {
        super.handleNode(node, element, uri, localName, parser);
        SubProcessNode subProcessNode = (SubProcessNode) node;
        String processId = element.getAttribute("calledElement");
        if (processId != null && processId.length() > 0) {
            subProcessNode.setProcessId(processId);
        } else {
            String processName = element.getAttribute("calledElementByName");
            subProcessNode.setProcessName(processName);
        }
        String waitForCompletion = element.getAttribute("waitForCompletion");
        if (waitForCompletion != null && "false".equals(waitForCompletion)) {
            subProcessNode.setWaitForCompletion(false);
        }
        String independent = element.getAttribute("independent");
        if (independent != null && "true".equals(independent)) {
            subProcessNode.setIndependent(true);
        }
        String version = element.getAttribute("version");
        if (version != null && !version.trim().isEmpty()) {
            subProcessNode.setProcessVersion(version);
        }
        Map<String, String> dataInputs = new HashMap<String, String>();
        Map<String, String> dataOutputs = new HashMap<String, String>();
        org.w3c.dom.Node xmlNode = element.getFirstChild();
        while (xmlNode != null) {
            String nodeName = xmlNode.getNodeName();
            if ("ioSpecification".equals(nodeName)) {
                readIoSpecification(xmlNode, dataInputs, dataOutputs, dataInputTypes, dataOutputTypes, parser);
            } else if ("dataInputAssociation".equals(nodeName)) {
                readDataInputAssociation(xmlNode, subProcessNode, dataInputs);
            } else if ("dataOutputAssociation".equals(nodeName)) {
                readDataOutputAssociation(xmlNode, subProcessNode, dataOutputs);
            }
            xmlNode = xmlNode.getNextSibling();
        }

        subProcessNode.setMetaData("DataInputs", dataInputs);
        subProcessNode.setMetaData("DataOutputs", dataOutputs);

        handleScript(subProcessNode, element, "onEntry");
        handleScript(subProcessNode, element, "onExit");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object end(String uri, String localName, ExtensibleXmlParser parser) throws SAXException {
        final Element element = parser.endElementBuilder();
        Node node = (Node) parser.getCurrent();
        handleNode(node, element, uri, localName, parser);

        org.w3c.dom.Node xmlNode = element.getFirstChild();
        int uniqueIdGen = 1;
        while (xmlNode != null) {
            String nodeName = xmlNode.getNodeName();
            if ("multiInstanceLoopCharacteristics".equals(nodeName)) {
                // create new timerNode
                ForEachNode forEachNode = new ForEachNode();
                forEachNode.setId(node.getId());
                String uniqueId = (String) node.getMetaData().get("UniqueId");
                forEachNode.setMetaData("UniqueId", uniqueId);
                node.setMetaData("UniqueId", uniqueId + ":" + uniqueIdGen++);
                node.setMetaData("hidden", true);
                forEachNode.addNode(node);
                forEachNode.linkIncomingConnections(NodeImpl.CONNECTION_DEFAULT_TYPE, node.getId(),
                        NodeImpl.CONNECTION_DEFAULT_TYPE);
                forEachNode.linkOutgoingConnections(node.getId(), NodeImpl.CONNECTION_DEFAULT_TYPE,
                        NodeImpl.CONNECTION_DEFAULT_TYPE);
                forEachNode.setSequential(Boolean.parseBoolean(((Element) xmlNode).getAttribute("isSequential")));

                Node orignalNode = node;
                node = forEachNode;
                handleForEachNode(node, element, uri, localName, parser);
                // remove output collection data output of for each to avoid problems when
                // running in variable strict mode
                if (orignalNode instanceof SubProcessNode) {
                    ((SubProcessNode) orignalNode).adjustOutMapping(forEachNode.getOutputCollectionExpression());
                }

                Map<String, String> dataInputs = (Map<String, String>) orignalNode.getMetaData().remove("DataInputs");
                Map<String, String> dataOutputs = (Map<String, String>) orignalNode.getMetaData().remove("DataOutputs");

                orignalNode.setMetaData("MICollectionOutput",
                        dataOutputs.get(((ForEachNode) node).getMetaData("MICollectionOutput")));
                orignalNode.setMetaData("MICollectionInput",
                        dataInputs.get(((ForEachNode) node).getMetaData("MICollectionInput")));

                break;
            }
            xmlNode = xmlNode.getNextSibling();
        }

        NodeContainer nodeContainer = (NodeContainer) parser.getParent();
        nodeContainer.addNode(node);
        ((ProcessBuildData) parser.getData()).addNode(node);
        return node;
    }

    protected void readIoSpecification(org.w3c.dom.Node xmlNode, Map<String, String> dataInputs,
            Map<String, String> dataOutputs, Map<String, String> dataInputTypes, Map<String, String> dataOutputTypes,
            ExtensibleXmlParser parser) {
        org.w3c.dom.Node subNode = xmlNode.getFirstChild();
        while (subNode instanceof Element) {
            String subNodeName = subNode.getNodeName();
            if ("dataInput".equals(subNodeName)) {
                String id = ((Element) subNode).getAttribute("id");
                String inputName = ((Element) subNode).getAttribute("name");
                dataInputs.put(id, inputName);
                String type = ((Element) subNode).getAttribute("dtype");
                String itemSubjectRef = ((Element) subNode).getAttribute("itemSubjectRef");
                dataInputTypes.put(inputName, retrieveDataType(itemSubjectRef, type, parser));
            } else if ("dataOutput".equals(subNodeName)) {
                String id = ((Element) subNode).getAttribute("id");
                String outputName = ((Element) subNode).getAttribute("name");
                dataOutputs.put(id, outputName);
                String type = ((Element) subNode).getAttribute("dtype");
                String itemSubjectRef = ((Element) subNode).getAttribute("itemSubjectRef");
                dataOutputTypes.put(outputName, retrieveDataType(itemSubjectRef, type, parser));
            }
            subNode = subNode.getNextSibling();
        }
    }

    protected void readDataInputAssociation(org.w3c.dom.Node xmlNode, SubProcessNode subProcessNode,
            Map<String, String> dataInputs) {

        // sourceRef
        org.w3c.dom.Node subNode = xmlNode.getFirstChild();
        if ("sourceRef".equals(subNode.getNodeName())) {

            String from = subNode.getTextContent();
            // targetRef
            subNode = subNode.getNextSibling();
            String to = subNode.getTextContent();
            // transformation
            Transformation transformation = null;
            subNode = subNode.getNextSibling();
            if (subNode != null && "transformation".equals(subNode.getNodeName())) {
                String lang = subNode.getAttributes().getNamedItem("language").getNodeValue();
                String expression = subNode.getTextContent();

                DataTransformer transformer = transformerRegistry.find(lang);
                if (transformer == null) {
                    throw new IllegalArgumentException("No transformer registered for language " + lang);
                }
                transformation = new Transformation(lang, expression);

                subNode = subNode.getNextSibling();
            }
            subProcessNode.addInMapping(dataInputs.get(to), from, transformation);
        } else {
            // targetRef
            String to = subNode.getTextContent();
            // assignment
            subNode = subNode.getNextSibling();
            if (subNode != null) {
                org.w3c.dom.Node subSubNode = subNode.getFirstChild();
                NodeList nl = subSubNode.getChildNodes();
                if (nl.getLength() > 1) {
                    // not supported ?
                    subProcessNode.addInMapping(dataInputs.get(to), subSubNode.getTextContent());
                    return;
                } else if (nl.getLength() == 0) {
                    return;
                }
                Object result = null;
                Object from = nl.item(0);
                if (from instanceof Text) {
                    result = ((Text) from).getTextContent();
                } else {
                    result = nl.item(0);
                }
                subProcessNode.addInMapping(dataInputs.get(to), result.toString());

            }
        }
    }

    protected void readDataOutputAssociation(org.w3c.dom.Node xmlNode, SubProcessNode subProcessNode,
            Map<String, String> dataOutputs) {
        // sourceRef
        org.w3c.dom.Node subNode = xmlNode.getFirstChild();
        String from = subNode.getTextContent();
        // targetRef
        subNode = subNode.getNextSibling();
        String to = subNode.getTextContent();
        // transformation
        Transformation transformation = null;
        subNode = subNode.getNextSibling();
        if (subNode != null && "transformation".equals(subNode.getNodeName())) {
            String lang = subNode.getAttributes().getNamedItem("language").getNodeValue();
            String expression = subNode.getTextContent();
            DataTransformer transformer = transformerRegistry.find(lang);
            if (transformer == null) {
                throw new IllegalArgumentException("No transformer registered for language " + lang);
            }
            transformation = new Transformation(lang, expression, from);
            subNode = subNode.getNextSibling();
        }
        subProcessNode.addOutMapping(dataOutputs.get(from), to, transformation);
    }

    protected void handleForEachNode(final Node node, final Element element, final String uri, final String localName,
            final ExtensibleXmlParser parser) throws SAXException {
        ForEachNode forEachNode = (ForEachNode) node;
        org.w3c.dom.Node xmlNode = element.getFirstChild();

        while (xmlNode != null) {
            String nodeName = xmlNode.getNodeName();
            if ("dataInputAssociation".equals(nodeName)) {
                readDataInputAssociation(xmlNode, inputAssociation);
            } else if ("dataOutputAssociation".equals(nodeName)) {
                readDataOutputAssociation(xmlNode, outputAssociation);
            } else if ("multiInstanceLoopCharacteristics".equals(nodeName)) {
                readMultiInstanceLoopCharacteristics(xmlNode, forEachNode, parser);
            }
            xmlNode = xmlNode.getNextSibling();
        }
    }

    public void writeNode(Node node, StringBuilder xmlDump, int metaDataType) {
        SubProcessNode subProcessNode = (SubProcessNode) node;
        writeNode("callActivity", subProcessNode, xmlDump, metaDataType);
        if (subProcessNode.getProcessId() != null) {
            xmlDump.append("calledElement=\""
                    + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(subProcessNode.getProcessId()) + "\" ");
        }
        if (!subProcessNode.isWaitForCompletion()) {
            xmlDump.append("tns:waitForCompletion=\"false\" ");
        }
        if (!subProcessNode.isIndependent()) {
            xmlDump.append("tns:independent=\"false\" ");
        }
        xmlDump.append(">" + EOL);
        writeExtensionElements(subProcessNode, xmlDump);
        writeIO(subProcessNode, xmlDump);
        endNode("callActivity", xmlDump);
    }

    protected void writeIO(SubProcessNode subProcessNode, StringBuilder xmlDump) {
        xmlDump.append("      <ioSpecification>" + EOL);
        for (Map.Entry<String, String> entry : subProcessNode.getInMappings().entrySet()) {
            xmlDump.append("        <dataInput id=\"" + XmlBPMNProcessDumper.getUniqueNodeId(subProcessNode) + "_"
                    + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(entry.getKey()) + "Input\" name=\""
                    + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(entry.getKey()) + "\" />" + EOL);
        }
        for (Map.Entry<String, String> entry : subProcessNode.getOutMappings().entrySet()) {
            xmlDump.append("        <dataOutput id=\"" + XmlBPMNProcessDumper.getUniqueNodeId(subProcessNode) + "_"
                    + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(entry.getKey()) + "Output\" name=\""
                    + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(entry.getKey()) + "\" />" + EOL);
        }
        xmlDump.append("        <inputSet>" + EOL);
        for (Map.Entry<String, String> entry : subProcessNode.getInMappings().entrySet()) {
            xmlDump.append("          <dataInputRefs>" + XmlBPMNProcessDumper.getUniqueNodeId(subProcessNode) + "_"
                    + XmlDumper.replaceIllegalChars(entry.getKey()) + "Input</dataInputRefs>" + EOL);
        }
        xmlDump.append("        </inputSet>" + EOL);
        xmlDump.append("        <outputSet>" + EOL);
        for (Map.Entry<String, String> entry : subProcessNode.getOutMappings().entrySet()) {
            xmlDump.append("          <dataOutputRefs>" + XmlBPMNProcessDumper.getUniqueNodeId(subProcessNode) + "_"
                    + XmlDumper.replaceIllegalChars(entry.getKey()) + "Output</dataOutputRefs>" + EOL);
        }
        xmlDump.append("        </outputSet>" + EOL);
        xmlDump.append("      </ioSpecification>" + EOL);
        for (Map.Entry<String, String> entry : subProcessNode.getInMappings().entrySet()) {
            xmlDump.append("      <dataInputAssociation>" + EOL);
            xmlDump.append("        <sourceRef>" + XmlDumper.replaceIllegalChars(entry.getValue()) + "</sourceRef>"
                    + EOL + "        <targetRef>" + XmlBPMNProcessDumper.getUniqueNodeId(subProcessNode) + "_"
                    + XmlDumper.replaceIllegalChars(entry.getKey()) + "Input</targetRef>" + EOL);
            xmlDump.append("      </dataInputAssociation>" + EOL);
        }
        for (Map.Entry<String, String> entry : subProcessNode.getOutMappings().entrySet()) {
            xmlDump.append("      <dataOutputAssociation>" + EOL);
            xmlDump.append("        <sourceRef>" + XmlBPMNProcessDumper.getUniqueNodeId(subProcessNode) + "_"
                    + XmlDumper.replaceIllegalChars(entry.getKey()) + "Output</sourceRef>" + EOL + "        <targetRef>"
                    + entry.getValue() + "</targetRef>" + EOL);
            xmlDump.append("      </dataOutputAssociation>" + EOL);
        }
    }

}
