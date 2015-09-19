package servicenow.common.datamart;

import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;

import servicenow.common.datamart.JobOperation;
import servicenow.common.datamart.LoadMethod;

import servicenow.common.soap.Record;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.Table;
import servicenow.common.soap.XMLFormatter;

public class Message {

	private final Table table;
	private final String sqlTableName;
	private final JobOperation operation;
	private final LoadMethod loadMethod;
	private final RecordList recs;
	
	Message(
			Table table, 
			String sqlTableName,
			JobOperation operation,
			LoadMethod loadMethod, 
			RecordList recs) {
		this.table = table;
		this.sqlTableName = sqlTableName;
		this.operation = operation;
		this.loadMethod = loadMethod;
		this.recs = recs;
	}
	
	private void addTextNode(Element ele, String name, String value) {
		Element node = new Element(name);
		node.setText(value);
		ele.addContent(node);
	}
	
	Element asElement() {
		Element eleRoot = new Element("transaction");
		addTextNode(eleRoot, "table", this.table.getName());
		addTextNode(eleRoot, "sqltablename", this.sqlTableName);
		addTextNode(eleRoot, "operation", this.operation.toString());
		if (this.operation.equals(JobOperation.LOAD))
			addTextNode(eleRoot, "loadmethod", this.loadMethod.toString());
		if (operation.equals(JobOperation.LOAD) || 
				operation.equals(JobOperation.REFRESH)) {
			Element eleRecs = new Element("records");
			eleRoot.addContent(eleRecs);
			for (Record rec : recs) {
				Element eleRec = new Element("record");
				eleRecs.addContent(eleRec);
				for (Map.Entry<String,String> entry : rec.getAllFields().entrySet()) {
					addTextNode(eleRec, entry.getKey(), entry.getValue());
				}
			}
		}
		return eleRoot;
	}
	
	Document asDocument() {		
		Element root = asElement();
		return new Document(root);
	}
	
	String asText() {
		Document doc = asDocument();
		return XMLFormatter.format(doc);
	}
}
