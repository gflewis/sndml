package servicenow.common.soap;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Properties;

import org.jdom2.JDOMException;
import org.junit.BeforeClass;
import org.junit.Test;

import servicenow.common.soap.InsufficientRightsException;
import servicenow.common.soap.InvalidTableNameException;
import servicenow.common.soap.Record;
import servicenow.common.soap.ServiceNowException;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;


public class SessionTest {

	static Properties config;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		config = AllTests.getProperties();
	}

	@Test
	public void testProperties() throws IOException {
		Properties prop = AllTests.getProperties();
		String username = prop.getProperty("servicenow.username");
		assertNotNull(username);
		assertTrue(username.equals("soap.junit") || username.equals("soap.sndm"));
	}
	
	@Test
	public void testGoodPassword() 
			throws ServiceNowException, IOException, JDOMException {
		String url = config.getProperty("servicenow.url");
		String username = config.getProperty("servicenow.username");
		String password = config.getProperty("servicenow.password");		
		Session session = new Session(url, username, password);
		assertNotNull(session);
		Table inc1 = session.table("incident");
		Table inc2 = session.table("incident");
		Table chg1 = session.table("change_request");
		Table chg2 = session.table("change_request");
		assertSame(inc1, inc2);
		assertSame(chg1, chg2);
		Record userProfile = session.getUserProfile();
		assertEquals(userProfile.getField("user_name"), username);
	}
	
	@Test (expected = InsufficientRightsException.class)
	public void testBadPassword() throws Exception {
		String url = config.getProperty("servicenow.url");
		String username = config.getProperty("servicenow.username");
		String badpassword = "bad";
		Session session = new Session(url, username, badpassword);
		assertNull(session);
	}

	@Test (expected = InvalidTableNameException.class)
	public void testBadTablename() throws Exception {
		Session session = AllTests.getSession();
		Table incident = session.table("inncident");
		assertNull(incident);
	}
	
	@Test
	public void testTableExistsTrue() throws Exception {
		Session session = AllTests.getSession();
		assertTrue(session.tableExists("incident"));
	}

	@Test
	public void testTableExistsFalse() throws Exception {
		Session session = AllTests.getSession();
		assertFalse(session.tableExists("innocent"));
	}
	
	@Test
	public void testTimeZone() throws Exception {
		Session session = AllTests.getSession();
		assertEquals("GMT", session.getTimeZone());
	}
}
