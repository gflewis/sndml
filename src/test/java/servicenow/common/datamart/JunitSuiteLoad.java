package servicenow.common.datamart;

import static org.junit.Assert.*;

import org.junit.*;

import java.sql.SQLException;

import org.slf4j.Logger;

import servicenow.common.datamart.Loader;
import servicenow.common.datamart.Status;

public class JunitSuiteLoad {

	static Logger log = AllTests.getLogger(JunitSuiteLoad.class);	
		
	@Test
	public void testSuiteStatus() throws Exception {
		Loader p = AllTests.newLoader();
		p.loadSuite("junit-location-load");
		Status status = p.getSuite().getStatus();
		assertNotNull(status);		
	}
		
	@Test (expected = Exception.class) 
	public void testEmptyJobSet() throws Exception {
		Loader p = AllTests.newLoader();
		p.loadSuite("blah blah blah");
	}

	@AfterClass
	public static void tearDownAfterClass() throws SQLException {
		DB.rollback();
	}
	
}
