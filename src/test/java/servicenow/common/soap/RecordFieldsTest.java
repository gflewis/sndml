package servicenow.common.soap;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import servicenow.common.soap.Record;
import servicenow.common.soap.Session;

public class RecordFieldsTest {

	static Logger logger = AllTests.junitLogger(RecordFieldsTest.class);
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testFieldNames() throws Exception {
		Session session = AllTests.getSession();
		Record userRec = session.getUserProfile();
		List<String> fields = userRec.getFieldNames();
		logger.info("count=" + fields.size());
		assertTrue(fields.size() > 60);
		assertEquals("active", fields.get(0));
	}

	@Test
	public void testFieldValues() throws Exception {
		Session session = AllTests.getSession();
		Record userRec = session.getUserProfile();
		Map<String,String> fields = userRec.getAllFields();
		for (String name : fields.keySet()) {
			String value = fields.get(name);
			logger.info(name + "=" + value);
		}
		assertEquals("GMT", fields.get("time_zone"));
	}
}
