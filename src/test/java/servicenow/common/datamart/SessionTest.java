package servicenow.common.datamart;

import static org.junit.Assert.*;

import java.sql.Connection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import servicenow.common.soap.Session;

public class SessionTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		AllTests.initialize();
	}

	@Test
	public void testGetSession() throws Exception {
		Session session = AllTests.getSession();
		assertNotNull(session);
		Connection connection = AllTests.getConnection();
		assertNotNull(connection);
	}

}
