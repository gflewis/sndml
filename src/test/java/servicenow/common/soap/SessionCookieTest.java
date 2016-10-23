package servicenow.common.soap;

import static org.junit.Assert.*;

import org.junit.*;

import servicenow.common.soap.Session;
import servicenow.common.soap.Table;

public class SessionCookieTest {

	Session instance;
	
	@Before
	public void setUp() throws Exception {
		instance = AllTests.getSession();
	}

	@Test
	public void testSession() throws Exception {
		Table location = instance.table("cmn_location");
		location.get("name", AllTests.getProperty("location1"));;
		String session1 = instance.getSessionID();
		System.out.println("JSESSIONID=" + session1);
		location.get("name", AllTests.getProperty("location2"));
		String session2 = instance.getSessionID();
		System.out.println("JSESSIONID=" + session2);
		location.get("name", AllTests.getProperty("location3"));
		String session3 = instance.getSessionID();
		System.out.println("JSESSIONID=" + session3);
		assertEquals(session1, session2);
		assertEquals(session2, session3);
	}

}
