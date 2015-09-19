package servicenow.common.soap;

import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import servicenow.common.soap.Session;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableReader;

public class PropertiesTest {

	static Logger log;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		log = AllTests.junitLogger(PropertiesTest.class);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testEmailLimit() throws Exception {
		Session instance = AllTests.getSession();
		Table sys_email = instance.table("sys_email");
		TableReader reader = sys_email.reader();
		int chunkSize = reader.chunkSize;
		log.info("limit=" + chunkSize);
		assertTrue(chunkSize > 0);
		assertTrue(chunkSize <= 50);
	}
	
}
