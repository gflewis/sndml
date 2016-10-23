package servicenow.common.soap;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import servicenow.common.soap.FieldValues;
import servicenow.common.soap.InvalidFieldNameException;
import servicenow.common.soap.InvalidTableNameException;
import servicenow.common.soap.Record;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableReader;

public class ValidateTest {

	static Session session;
	static String incident_number;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		session = AllTests.getSession();
		// instance.setValidate(true);
		incident_number = 
			AllTests.getProperty("some_incident_number");
	}

	@Test (expected = InvalidTableNameException.class)
	public void testBadTable() throws Exception {
		Table bad = session.table("incidentxxxx");
		fail("tablename=" + bad.getName());
	}
	
	@Test (expected = InvalidFieldNameException.class)
	public void testWriteValidation() throws Exception {
		Table inc = session.table("incident");
		Record rec = inc.get("number", incident_number);
	    new FieldValues().	
	    	set("short_descriptionx", "This is a test").
	        update(inc, rec.getKey());
	}

	@Test (expected = InvalidFieldNameException.class)
	public void testReadValidation() throws Exception {
		Table inc = session.table("incident");
		Record rec = inc.get("number", incident_number);
		assertNotNull(rec);
		String descr = rec.getField("short_descriptionxx");
		assertNull(descr);
	}
	
	@Test (expected = InvalidFieldNameException.class)
	public void testSortValidation() throws Exception {
		Table usr = session.table("sys_user");
		TableReader f = usr.reader();
		f.sortAscending("updated_on");
	}
}
