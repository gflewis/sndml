package servicenow.common.soap;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class FieldNames extends ArrayList<String> {

	public FieldNames() {
		super();
	}

	public FieldNames(int size) {
		super(size);
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		String delim = "";
		for (String name : this) {
			result.append(delim).append(name);
			delim = ",";
		}
		return result.toString();
	}
}
