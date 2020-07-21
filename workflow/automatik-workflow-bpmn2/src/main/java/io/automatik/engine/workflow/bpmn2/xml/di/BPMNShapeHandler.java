
package io.automatik.engine.workflow.bpmn2.xml.di;

import java.util.HashSet;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatik.engine.workflow.bpmn2.xml.di.BPMNEdgeHandler.ConnectionInfo;
import io.automatik.engine.workflow.bpmn2.xml.di.BPMNPlaneHandler.ProcessInfo;
import io.automatik.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.compiler.xml.Handler;

public class BPMNShapeHandler extends BaseAbstractHandler implements Handler {

	public BPMNShapeHandler() {
		initValidParents();
		initValidPeers();
		this.allowNesting = true;
	}

	protected void initValidParents() {
		this.validParents = new HashSet<Class<?>>();
		this.validParents.add(ProcessInfo.class);
	}

	protected void initValidPeers() {
		this.validPeers = new HashSet<Class<?>>();
		this.validPeers.add(null);
		this.validPeers.add(NodeInfo.class);
		this.validPeers.add(ConnectionInfo.class);
	}

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);
		final String elementRef = attrs.getValue("bpmnElement");
		NodeInfo nodeInfo = new NodeInfo(elementRef);
		ProcessInfo processInfo = (ProcessInfo) parser.getParent();
		processInfo.addNodeInfo(nodeInfo);
		return nodeInfo;
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		Element element = parser.endElementBuilder();
		NodeInfo nodeInfo = (NodeInfo) parser.getCurrent();
		org.w3c.dom.Node xmlNode = element.getFirstChild();
		while (xmlNode instanceof Element) {
			String nodeName = xmlNode.getNodeName();
			if ("Bounds".equals(nodeName)) {
				// ignore first and last waypoint
				String x = ((Element) xmlNode).getAttribute("x");
				String y = ((Element) xmlNode).getAttribute("y");
				String width = ((Element) xmlNode).getAttribute("width");
				String height = ((Element) xmlNode).getAttribute("height");
				try {
					int xValue = 0;
					if (x != null && x.trim().length() != 0) {
						xValue = new Float(x).intValue();
					}
					int yValue = 0;
					if (y != null && y.trim().length() != 0) {
						yValue = new Float(y).intValue();
					}
					int widthValue = 20;
					if (width != null && width.trim().length() != 0) {
						widthValue = new Float(width).intValue();
					}
					int heightValue = 20;
					if (height != null && height.trim().length() != 0) {
						heightValue = new Float(height).intValue();
					}
					nodeInfo.setX(xValue);
					nodeInfo.setY(yValue);
					nodeInfo.setWidth(widthValue);
					nodeInfo.setHeight(heightValue);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Invalid bounds for node " + nodeInfo.getNodeRef(), e);
				}
			}
			xmlNode = xmlNode.getNextSibling();
		}
		return parser.getCurrent();
	}

	public Class<?> generateNodeFor() {
		return NodeInfo.class;
	}

	public static class NodeInfo {

		private String nodeRef;
		private Integer x;
		private Integer y;
		private Integer width;
		private Integer height;

		public NodeInfo(String nodeRef) {
			this.nodeRef = nodeRef;
		}

		public String getNodeRef() {
			return nodeRef;
		}

		public Integer getX() {
			return x;
		}

		public void setX(Integer x) {
			this.x = x;
		}

		public Integer getY() {
			return y;
		}

		public void setY(Integer y) {
			this.y = y;
		}

		public Integer getWidth() {
			return width;
		}

		public void setWidth(Integer width) {
			this.width = width;
		}

		public Integer getHeight() {
			return height;
		}

		public void setHeight(Integer height) {
			this.height = height;
		}

	}

}
