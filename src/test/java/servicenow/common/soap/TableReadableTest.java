package servicenow.common.soap;

import static org.junit.Assert.*;

import org.junit.*;

import servicenow.common.soap.Session;

public class TableReadableTest {

	@Test
	public void testReadable() throws Exception {
		Session session = AllTests.getSession();
		assertTrue(session.table("incident").isReadable());
		assertTrue(session.table("task").isReadable());
	}

}
