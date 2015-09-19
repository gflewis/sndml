package servicenow.common.soap;

import java.util.regex.Pattern;

import servicenow.common.soap.Table;

/**
 * 
 * Thin wrapper for a <b>sys_id</b> (GUID).  
 * This class is used to ensure proper parameter type resolution 
 * for various methods in the {@link Table} class.
 *
 */
public final class Key {

	static Pattern pattern = Pattern.compile("[0-9a-f]{32}");
	final String value;	
	
	public Key(String value) {
		assert value != null;
		this.value = value;
	}
	
	public String toString() {
		return this.value;
	}
	
	public boolean equals(Object other) {
		return this.value.equals(other.toString());
	}
		
	static public boolean isValidGUID(String v) {
		return pattern.matcher(v).matches();
	}
	
	public boolean isValidGUID() {
		return isValidGUID(value);
	}
}

