
package io.automatik.engine.workflow.process.core.node;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.automatik.engine.workflow.base.core.context.variable.Mappable;

import java.util.LinkedList;
import java.util.List;

public class Trigger implements Mappable, Serializable {

	private static final long serialVersionUID = 510l;

	private List<DataAssociation> inMapping = new LinkedList<DataAssociation>();

	public void addInMapping(String subVariableName, String variableName) {
		inMapping.add(new DataAssociation(subVariableName, variableName, null, null));
	}

	public void setInMappings(Map<String, String> inMapping) {
		this.inMapping = new LinkedList<DataAssociation>();
		for (Map.Entry<String, String> entry : inMapping.entrySet()) {
			addInMapping(entry.getKey(), entry.getValue());
		}
	}

	public String getInMapping(String parameterName) {
		return getInMappings().get(parameterName);
	}

	public Map<String, String> getInMappings() {
		Map<String, String> in = new HashMap<String, String>();
		for (DataAssociation a : inMapping) {
			if (a.getSources().size() == 1 && (a.getAssignments() == null || a.getAssignments().size() == 0)
					&& a.getTransformation() == null) {
				in.put(a.getSources().get(0), a.getTarget());
			}
		}
		return in;
	}

	public void addInAssociation(DataAssociation dataAssociation) {
		inMapping.add(dataAssociation);
	}

	public List<DataAssociation> getInAssociations() {
		return Collections.unmodifiableList(inMapping);
	}

	public void addOutMapping(String subVariableName, String variableName) {
		throw new IllegalArgumentException("A trigger does not support out mappings");
	}

	public void setOutMappings(Map<String, String> outMapping) {
		throw new IllegalArgumentException("A trigger does not support out mappings");
	}

	public String getOutMapping(String parameterName) {
		throw new IllegalArgumentException("A trigger does not support out mappings");
	}

	public Map<String, String> getOutMappings() {
		throw new IllegalArgumentException("A trigger does not support out mappings");
	}

	public void addOutAssociation(DataAssociation dataAssociation) {
		throw new IllegalArgumentException("A trigger does not support out mappings");
	}

	public List<DataAssociation> getOutAssociations() {
		throw new IllegalArgumentException("A trigger does not support out mappings");
	}
}
