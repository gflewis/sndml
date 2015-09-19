package servicenow.common.datamart;

import static org.junit.Assert.*;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import servicenow.common.datamart.SuiteModel;

public class BatchInsertTest {

	static Logger log = AllTests.getLogger(BatchInsertTest.class);
	
	@Before
	public void setUpBefore() throws Exception {
		AllTests.initialize();
	}

	@Test
	public void testLoadGroups() throws Exception {
		String tname = "sys_user_group";
		log.info("testLoadGroups");
		DB.dropTable(tname);
		SuiteModel suite = AllTests.newSuite(
			"load " + tname + " truncate");
		suite.getController().runOnce();
		int count = DB.numRows(tname);
		log.info("count=" + count);
		assertTrue(count > 50);
	}

	@After
	public void tearDownAfter() throws SQLException {
		DB.rollback();
	}
		
}
