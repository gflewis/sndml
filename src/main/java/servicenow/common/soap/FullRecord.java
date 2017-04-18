package servicenow.common.soap;

import org.jdom2.Element;

public class FullRecord extends Record {

	public FullRecord(Table table, Element element) throws SoapResponseException {
		super(table, element);
		if (this.key == null)
			throw new SoapResponseException(this.table, 
				"Missing sys_id in SOAP response" + 
				" (probably a missing Access Control)", getXML());
		if (this.createdTimestamp == null)
			throw new SoapResponseException(this.table, 
				"Missing sys_created_on", getXML());
	}

}
