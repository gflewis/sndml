package servicenow.common.soap;

import org.junit.*;

import static org.junit.Assert.*;

import java.util.List;

import org.slf4j.Logger;

import servicenow.common.soap.InvalidTableNameException;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableWSDL;

public class WSDLTest {

	static Logger logger = AllTests.junitLogger(WSDLTest.class);
	
	@Test
	public void testGoodTable() throws Exception {
		Session session = AllTests.getSession();
		String tablename = "incident";
		TableWSDL wsdl = new TableWSDL(session, tablename);
		List<String> columns = wsdl.getReadColumnNames();
		int count = columns.size();
		logger.info(tablename + " has " + count + " columns");
		assert(count > 60);
	}

	@Test (expected = InvalidTableNameException.class)
	public void badTableTest() throws Exception {
		Session session = AllTests.getSession();
		String tablename = "incidentxxx";
		@SuppressWarnings("unused")
		TableWSDL wsdl = new TableWSDL(session, tablename);
		fail();
	}
	
	@Test
	public void testDefaultWSDL() throws Exception {
		Table table = AllTests.getSession().table("incident");
		TableWSDL wsdl = table.getWSDL();
		assertTrue(wsdl.canReadField("sys_updated_on"));
		assertFalse(wsdl.canReadField("dv_assigned_to"));
		assertFalse(wsdl.canReadField("createdxxxxx"));
		assertTrue(wsdl.canWriteField("short_description"));
		assertFalse(wsdl.canWriteField("short_descriptionxxx"));
	}

	@Test
	public void testDisplayValues() throws Exception {
		Table table = AllTests.getSession().table("incident");
		TableWSDL wsdl = table.getWSDL(true);
		assertTrue(wsdl.canReadField("sys_updated_on"));
		assertTrue(wsdl.canReadField("dv_assigned_to"));
		assertFalse(wsdl.canReadField("createdxxxxx"));
		assertTrue(wsdl.canWriteField("short_description"));
		assertFalse(wsdl.canWriteField("short_descriptionxxx"));
	}

	@Test
	public void testDisplayValues2() throws Exception {
		Table table = AllTests.getSession().table("incident");
		table.setDisplayValues(true);
		TableWSDL wsdl = table.getWSDL();
		assertTrue(wsdl.canReadField("sys_updated_on"));
		assertTrue(wsdl.canReadField("dv_assigned_to"));
		assertFalse(wsdl.canReadField("createdxxxxx"));
	}
	
}
