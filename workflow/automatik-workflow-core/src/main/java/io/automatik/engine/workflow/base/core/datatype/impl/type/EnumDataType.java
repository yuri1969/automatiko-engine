package io.automatik.engine.workflow.base.core.datatype.impl.type;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import io.automatik.engine.workflow.base.core.datatype.DataType;

/**
 * Representation of an Enum datatype.
 */
public class EnumDataType implements DataType {

	private static final long serialVersionUID = 4L;

	private String className;
	private transient Map<String, Object> valueMap;

	private Class<?> clazz;

	public EnumDataType() {
	}

	public EnumDataType(String className) {
		setClassName(className);
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
		try {
			this.clazz = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Error creating class of " + className, e);
		}
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		className = (String) in.readObject();
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(className);
	}

	public boolean verifyDataType(final Object value) {
		if (value == null) {
			return true;
		}
		return getValueMap(null).containsValue(value);
	}

	public Object readValue(String value) {
		return getValueMap(null).get(value);
	}

	public String writeValue(Object value) {
		return value == null ? "" : value.toString();
	}

	public String getStringType() {
		return className == null ? "java.lang.Object" : className;
	}

	public Object[] getValues(ClassLoader classLoader) {
		return getValueMap(classLoader).values().toArray();
	}

	public Object[] getValues() {
		return getValues(null);
	}

	public String[] getValueNames(ClassLoader classLoader) {
		return getValueMap(classLoader).keySet().toArray(new String[0]);
	}

	public String[] getValueNames() {
		return getValueNames(null);
	}

	public Map<String, Object> getValueMap() {
		return getValueMap(null);
	}

	public Map<String, Object> getValueMap(ClassLoader classLoader) {
		if (this.valueMap == null) {
			try {
				this.valueMap = new HashMap<String, Object>();
				if (className == null) {
					return null;
				}
				Class<?> clazz = classLoader == null ? Class.forName(className)
						: Class.forName(className, true, classLoader);
				if (!clazz.isEnum()) {
					return null;
				}
				Object[] values = (Object[]) clazz.getMethod("values", null).invoke(clazz, null);
				for (Object value : values) {
					this.valueMap.put(value.toString(), value);
				}
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Could not find data type " + className);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException("IllegalAccessException " + e);
			} catch (InvocationTargetException e) {
				throw new IllegalArgumentException("InvocationTargetException " + e);
			} catch (NoSuchMethodException e) {
				throw new IllegalArgumentException("NoSuchMethodException " + e);
			}

		}
		return this.valueMap;
	}

	@Override
	public Class<?> getClassType() {
		return clazz;
	}
}
