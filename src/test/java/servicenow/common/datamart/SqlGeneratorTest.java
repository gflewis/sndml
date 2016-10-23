package servicenow.common.datamart;

import static org.junit.Assert.*;

import org.junit.*;

import java.sql.SQLException;
import java.util.HashMap;
import org.slf4j.Logger;

import servicenow.common.datamart.DatabaseWriter;
import servicenow.common.datamart.DatamartConfiguration;
import servicenow.common.datamart.SqlGenerator;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;

public class SqlGeneratorTest {

	static Logger logger = AllTests.getLogger(SqlGeneratorTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testUppercaseName() throws Exception {
		DatamartConfiguration config = AllTests.getConfiguration();
		config.setProperty("dialect", "oracle2");
		SqlGenerator generator = new SqlGenerator(config, AllTests.getDBWriter());
		String sqlname = generator.sqlName("user");
		assertEquals("USER_", sqlname);		
	}

	@Test
	public void testQuotedName() throws Exception {
		DatamartConfiguration config = AllTests.getConfiguration();
		config.setProperty("dialect", "oracle");
		SqlGenerator generator = new SqlGenerator(config, AllTests.getDBWriter());
		String sqlname = generator.sqlName("user");
		assertEquals("\"USER\"", sqlname);		
	}
	
	@Test
	public void testSchema() throws Exception {
		DatamartConfiguration config = AllTests.getConfiguration();
		config.setProperty("schema", "xxxx");
		DatabaseWriter database = new DatabaseWriter(config);
		SqlGenerator generator = new SqlGenerator(config, database);
		HashMap<String,String> map = new HashMap<String,String>();
		map.put("fieldmap", "name=?");
		map.put("keyvalue", "?");
		String updateSql = generator.getTemplate("update", "incident", map);
		logger.info(updateSql);
		assertTrue(updateSql.toUpperCase().startsWith("UPDATE XXXX."));		
	}
	
	@Test
	public void testNullSchema() throws Exception {
		DatamartConfiguration config = AllTests.getConfiguration();
		config.setProperty("schema", "");
		DatabaseWriter database = new DatabaseWriter(config);
		SqlGenerator generator = new SqlGenerator(config, database);
		HashMap<String,String> map = new HashMap<String,String>();
		map.put("fieldmap", "name=?");
		map.put("keyvalue", "?");
		String updateSql = generator.getTemplate("update", "incident", map);
		logger.info(updateSql);
		assertFalse(updateSql.startsWith("UPDATE ."));		
	}
	
	@Test
	public void testCreateTable() throws Exception {
		DatamartConfiguration config = AllTests.getConfiguration();
		final String tablename = "incident";
		config.setProperty("autocreate", "false");
		DatabaseWriter database = new DatabaseWriter(config);
		SqlGenerator generator = new SqlGenerator(config, database);
		Session session = AllTests.getSession();
		assertNotNull(session);
		Table table = session.table(tablename);
		String createSql = generator.getCreateTable(table, tablename, false);
		logger.info(createSql);
		assertTrue(createSql.toUpperCase().startsWith("CREATE TABLE"));
	}
	
	@After
	public void tearDown() throws SQLException {
		DB.rollback();
	}
		
}
