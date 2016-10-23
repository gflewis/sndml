package servicenow.common.soap;

import org.jdom2.Element;
import org.jdom2.Namespace;

import servicenow.common.soap.Key;

/**
 * Holds the result from an <b>insert</b> Web Services call.
 * <p/>
 * For details refer to 
 * <a href="http://wiki.servicenow.com/index.php?title=Direct_Web_Service_API_Functions#insert"
 * >http://wiki.servicenow.com/index.php?title=Direct_Web_Service_API_Functions#insert</a>
 * @author Giles Lewis
 *
 */
public class InsertResponse {

	private final Element element;
	private final Namespace ns;
	
	protected InsertResponse(Element insertResponse) {
		this.element = insertResponse;
		this.ns = this.element.getNamespace();
	}
	
	public Key getSysId() {		
		String sysid = element.getChildText("sys_id", ns);
		return (sysid == null) ? null : new Key(sysid);
	}
	
	public String getNumber() {
		return element.getChildText("number");
	}
	
	/**
	 * Returns "inserted", "updated" or "error" if the target
	 * table was an Import Set Table. Otherwise returns null;
	 */
	public String getStatus() {
		return element.getChildText("status", ns);
	}
	
	/**
	 * Returns null unless the target table was an Import Set Table.
	 */
	public String getStatusMessage() {
		return element.getChildText("status_message", ns);
	}
	
	public String getDisplayName() {
		return element.getChildText("display_name", ns);
	}
	
	public String getDisplayValue() {
		return element.getChildText("display_value", ns);
	}
	
	public String getTable() {
		return element.getChildText("table", ns);
	}
}
