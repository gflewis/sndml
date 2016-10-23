package servicenow.common.soap;

import org.junit.*;

import servicenow.common.soap.FieldValues;
import servicenow.common.soap.InvalidTableNameException;
import servicenow.common.soap.Key;
import servicenow.common.soap.Record;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;

import static org.junit.Assert.*;

public class TableTest {

	static Session session;
	static String inc_number;
	
	@BeforeClass 
	public static void initialize() throws Exception {
		session = AllTests.getSession();
		inc_number = AllTests.someIncidentNumber();  		
	}
	
	@Test
	public void testGetParentName() throws Exception {
		Table inc = session.table("incident");
		assertEquals("task", inc.getSchema().getParentName());
	}

	@Test (expected = InvalidTableNameException.class)
	public void testBadTableName() throws Exception {
		session.table("incidentxxx");
	}
	
	@Test
	public void testGetReference() throws Exception {
		Table inc = session.table("incident");
		Record rec = inc.get("number", inc_number);
		assertNotNull(rec);
		String caller = rec.getRecord("caller_id").getField("name");
		assertNotNull(caller);
	}
	
	@Test
	public void testDotWalk() throws Exception {
		String name =
			session.table("sys_user_group").
				get("name", AllTests.someGroup()).
				getRecord("manager").
				getField("name");
		assertNotNull(name);
	}
	
	@Test (expected = NullPointerException.class)
	public void testDotWalkException() throws Exception {
		session.table("sys_user_group").
				get("name", "Network Supporttt").
				getRecord("manager").
				getField("name");
	}
	
	@Test
	public void testInsertDelete() throws Exception {
		Table tbl = session.table("incident");
	    FieldValues values = new FieldValues();
	    values.put("short_description", "This is a test");
	    Key sysid = tbl.insert(values).getSysId();
	    assertNotNull(sysid);
	    assertTrue("Delete record just inserted", tbl.deleteRecord(sysid));
	    assertFalse("Delete non-existent record", tbl.deleteRecord(sysid));
	}

	@Test
	public void testGetRecords() throws Exception {
		Table tbl = session.table("cmn_department");
		RecordList recs = tbl.getRecords("id", 
			AllTests.getProperty("some_department_id"));
		assertTrue(recs.size() > 0);
	}
	
	@Test
	public void testGetEmptyRecordset() throws Exception {
		Table tbl = session.table("sys_user");
		RecordList recs = tbl.getRecords("name", "Zebra Elephant");
		assertTrue(recs.size() == 0);
	}
}
