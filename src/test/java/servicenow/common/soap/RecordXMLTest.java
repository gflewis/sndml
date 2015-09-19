package servicenow.common.soap;

import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.jdom2.Element;
import org.junit.Test;

import servicenow.common.soap.Record;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;

public class RecordXMLTest {

	Logger logger = AllTests.junitLogger(RecordXMLTest.class);
	
	@Test
	public void testRecordXML() throws Exception {
		Session session = AllTests.getSession();
		Table tbl = session.table("cmn_location");
		String locname = AllTests.getProperty("location1");
		Record rec = tbl.get("name", locname);
		assertNotNull(rec);
		String xml1 = rec.getXML(true);
		assertTrue(xml1.startsWith("<getRecordsResult>"));
		Element ele = rec.getElement();
		assertTrue(ele.getName().equals(tbl.getName()));
		assertTrue(ele.getChildText("sys_id").length() == 32);
		assertTrue(ele.getChildText("name").equals(locname));
	}

}
