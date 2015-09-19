package servicenow.common.soap;

import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import servicenow.common.soap.FieldValues;
import servicenow.common.soap.FieldValuesList;
import servicenow.common.soap.InsertMultipleResponse;
import servicenow.common.soap.InsertResponse;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;

public class InsertMultipleTest {

	static Logger logger = AllTests.junitLogger(InsertMultipleTest.class);
	Session session;
	Table incident;
	InsertMultipleResponse responses;
	
	@Before
	public void setUp() throws Exception {
		session = AllTests.getSession();
		incident = session.table("incident");
	}
	
	@Test
	public void testInsertIncident() throws Exception {
		Table incident = session.table("incident");
		FieldValuesList list = new FieldValuesList();
		FieldValues values = new FieldValues();
		values.set("short_description", "Multi-Insert Test 1");
		list.add(values);
		values.set("short_description", "Multi-Insert Test 2");
		list.add(values);
		responses = incident.insertMultiple(list);
		for (InsertResponse resp : responses) {
			String number = resp.getNumber();
			logger.info("Inserted " + number);
			assertTrue(number.length() > 5);
			assertNull(resp.getStatus());
		}
		assertEquals(2, responses.size());
	}

	@After
	public void tearDown() throws Exception {
		for (InsertResponse resp : responses) {
			logger.info("Deleting " + resp.getNumber());
			incident.deleteRecord(resp.getSysId());
		}
	}
	
}
