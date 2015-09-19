package servicenow.common.datamart;

import static org.junit.Assert.*;

import org.junit.*;

import java.sql.SQLException;

import org.slf4j.Logger;

import servicenow.common.datamart.Loader;

public class SimpleLoadTest {

	static Logger log = AllTests.getLogger(SimpleLoadTest.class);
	
	int loadTable(String tableName) throws Exception {
		String sqlTableName = DB.tableName(tableName);
		log.info("dropping table");
		DB.dropTable(sqlTableName);
		log.info("starting load");
		Loader loader = AllTests.newLoader("load " + tableName);
		loader.runToComplete();
		log.info("load complete");
		return DB.sqlCountTable(sqlTableName);		
	}

	@Before
	public void setUpBefore() throws Exception {
		AllTests.initialize();
	}
	
	@Test
	public void loadTest() throws Exception {
		int count = loadTable("cmn_department");
		assertTrue(count > 50);
	}

	@After
	public void tearDown() throws SQLException {
		DB.rollback();
	}
	
}
