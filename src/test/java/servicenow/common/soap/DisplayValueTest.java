package servicenow.common.soap;

import static org.junit.Assert.*;

import org.junit.*;

import servicenow.common.soap.InvalidFieldNameException;
import servicenow.common.soap.Record;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;


public class DisplayValueTest {

	static Session instance;
	static String groupName;
	static String mgrName;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		instance = AllTests.getSession();
		// instance.setValidate(true);
		groupName = AllTests.someGroup();
		mgrName = AllTests.someGroupManger();
	}

	@Test (expected = InvalidFieldNameException.class)
	public void testDisplayValueFalse() throws Exception {
		Table grp = instance.table("sys_user_group");		
		grp.setDisplayValues(false);
		Record rec = grp.get("name", groupName);
		String mgr = rec.getField("dv_manager");
		assertNotNull(mgr);
	}

	@Test
	public void testDisplayValueTrue() throws Exception {
		Table grp = instance.table("sys_user_group");
		grp.setDisplayValues(true);
		Record rec = grp.get("name", groupName);
		String mgr = rec.getField("dv_manager");
		assertEquals(mgr, mgrName);
	}

	@Test 
	public void testGetDisplayValue() throws Exception {
		Table grp = instance.table("sys_user_group");
		grp.setDisplayValues(true);
		Record rec = grp.get("name", groupName);
		String mgr = rec.getDisplayValue("manager");
		assertEquals(mgr, mgrName);
	}
	
	@Test (expected = IllegalStateException.class) 
	public void testGetDisplayValueException() throws Exception {
		Table grp = instance.table("sys_user_group");
		grp.setDisplayValues(false);
		Record rec = grp.get("name", groupName);
		String mgr = rec.getDisplayValue("manager");
		assertEquals(mgr, mgrName);		
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		Table grp = instance.table("sys_user_group");		
		grp.setDisplayValues(false);
	}
	
}
