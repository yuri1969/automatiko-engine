
package io.automatik.engine.workflow.compiler.xml.processes;

import java.util.HashSet;

import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatik.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.compiler.xml.Handler;
import io.automatik.engine.workflow.process.core.Constraint;
import io.automatik.engine.workflow.process.core.impl.ConnectionRef;
import io.automatik.engine.workflow.process.core.impl.ConstraintImpl;
import io.automatik.engine.workflow.process.core.impl.NodeImpl;
import io.automatik.engine.workflow.process.core.node.Constrainable;

public class ConstraintHandler extends BaseAbstractHandler implements Handler {

	public ConstraintHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet<Class<?>>();
			this.validParents.add(Constrainable.class);

			this.validPeers = new HashSet<Class<?>>();
			this.validPeers.add(null);

			this.allowNesting = false;
		}
	}

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);
		return null;
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		final Element element = parser.endElementBuilder();

		Constrainable parent = (Constrainable) parser.getParent();
		Constraint constraint = new ConstraintImpl();

		final String toNodeIdString = element.getAttribute("toNodeId");
		String toType = element.getAttribute("toType");
		ConnectionRef connectionRef = null;
		if (toNodeIdString != null && toNodeIdString.trim().length() > 0) {
			int toNodeId = new Integer(toNodeIdString);
			if (toType == null || toType.trim().length() == 0) {
				toType = NodeImpl.CONNECTION_DEFAULT_TYPE;
			}
			connectionRef = new ConnectionRef(toNodeId, toType);
		}

		final String name = element.getAttribute("name");
		constraint.setName(name);
		final String priority = element.getAttribute("priority");
		if (priority != null && priority.length() != 0) {
			constraint.setPriority(new Integer(priority));
		}

		final String type = element.getAttribute("type");
		constraint.setType(type);
		final String dialect = element.getAttribute("dialect");
		constraint.setDialect(dialect);

		String text = ((Text) element.getChildNodes().item(0)).getWholeText();
		if (text != null) {
			text = text.trim();
			if ("".equals(text)) {
				text = null;
			}
		}
		constraint.setConstraint(text);
		parent.addConstraint(connectionRef, constraint);
		return null;
	}

	@SuppressWarnings("unchecked")
	public Class generateNodeFor() {
		return Constraint.class;
	}

}
