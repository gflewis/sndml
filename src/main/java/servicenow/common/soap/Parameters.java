package servicenow.common.soap;

import java.util.*;

import servicenow.common.soap.Key;
import servicenow.common.soap.Parameters;

/**
 * This class holds a list of name/value pairs.
 * 
 * @author Giles Lewis
 */
@SuppressWarnings("serial")
class Parameters extends LinkedHashMap<String, String> {

	public Parameters() {
		super();
	}
	
	public Parameters(Parameters params) {
		super();
		if (params != null) this.putAll(params);
	}
	
	public Parameters(String name, String value) {
		super();
		super.put(name, value);
	}

	public void add(String name, String value) {
		super.put(name, value);
	}
	
	public void add(Parameters params) {
		if (params == null) return;
		super.putAll(params);
	}
	
	public boolean contains(String name) {
		return super.containsKey(name);
	}
	
	public Key getSysId() {
		String sysid = get("sys_id");
		return (sysid == null) ? null : new Key(sysid);
	}
	
	public String getNumber() {
		return get("number");
	}

}
