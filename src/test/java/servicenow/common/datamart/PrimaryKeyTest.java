package servicenow.common.datamart;

import static org.junit.Assert.*;

import org.junit.*;

import java.sql.SQLException;

import org.slf4j.Logger;

import servicenow.common.datamart.SuiteExecException;
import servicenow.common.datamart.SuiteModel;

public class PrimaryKeyTest {

	static Logger log = AllTests.getLogger(PrimaryKeyTest.class);

	@Before
	public void setUp() throws Exception {
		AllTests.initialize();
	}

	@Test (expected = SuiteExecException.class)
	public void test() throws Exception {
		log.info("PrimaryKeyTest");
		String c = AllTests.junitProperty("some_group_name").substring(0, 1);
		String[] jobs =	{
			"load sys_user_group truncate where {active=true^nameSTARTSWITH" + c + "}",
			"load sys_user_group insert-only order-by name "};
		SuiteModel suite = AllTests.newSuite(jobs);
		suite.getController().runOnce();
		fail("Exception not thrown");
	}

	@After
	public void tearDownAfter() throws SQLException {
		log.info("rollback");
		DB.rollback();
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws SQLException {
		log.info("rollback");
		DB.rollback();
	}
	
}
