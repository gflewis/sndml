package servicenow.common.soap;

import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.junit.Before;
import org.junit.Test;

import servicenow.common.soap.FieldValues;
import servicenow.common.soap.FieldValuesList;
import servicenow.common.soap.InsertMultipleResponse;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;

public class WSImportTest {

	static Logger logger = AllTests.junitLogger(WSImportTest.class);
	Session session;
	Table network;
	InsertMultipleResponse response;
	
	@Before
	public void setUp() throws Exception {
		session = AllTests.getSession();
	}
	
	@Test
	public void testInsertNetwork() throws Exception {
		network = session.table("u_imp_ws_ip_network");
		FieldValuesList list = new FieldValuesList();
		FieldValues values = new FieldValues();
		values.set("u_cidr_address", "1.1.1.1/30");
		values.set("u_infoblox_comment", "Blah Blah Blah");
		list.add(values);
		response = network.insertMultiple(list);
		assertTrue(response.size() > 2);
	}

}
