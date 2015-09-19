package servicenow.common.datamart;

import static org.junit.Assert.*;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import servicenow.common.datamart.LoadLimitExceededException;
import servicenow.common.datamart.Loader;

/**
 * Test loading a table with column names that are reserved words.
 */
public class ReservedWordsTest {

	static Logger logger = AllTests.getLogger(ReservedWordsTest.class);

	@Before
	public void setUp() throws Exception {
	}

	@Test 
	public void loadReservedWords() throws Exception {
		String tableName = "sys_user_grmember";
		String groupName = AllTests.junitProperty("some_group_name");
		logger.info("table='" + tableName + "' group='" + groupName + "'");
		DB.dropTable(tableName);
		String cmd = "load " + tableName + " where {group.name=" + groupName + "}";
		Loader loader = AllTests.newLoader(cmd);
		try {
			loader.runToComplete();
		} catch (LoadLimitExceededException e) {
			logger.warn(e.getMessage());
		};
		int count = DB.numRows(tableName);		
		assertTrue(count > 1);		
	}
	
	@After
	public void tearDown() throws SQLException {
		DB.rollback();
	}
	
}
