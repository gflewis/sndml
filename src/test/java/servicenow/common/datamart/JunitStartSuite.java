package servicenow.common.datamart;

import static org.junit.Assert.*;

import org.junit.*;

import java.io.IOException;
import java.sql.SQLException;

import org.slf4j.Logger;

import servicenow.common.soap.DateTime;
import servicenow.common.soap.FieldValues;
import servicenow.common.soap.Key;
import servicenow.common.soap.Record;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;

/**
 * When Suite Status is changed to Pending, u_run_start is updated
 */
public class JunitStartSuite {

	static Logger logger = AllTests.getLogger(JunitStartSuite.class);
	
	@Before
	public void setUpBefore() throws IOException  {
		AllTests.initialize();
	}
	
	@Test
	public void test() throws Exception {
		Session session = AllTests.getSession();
		Table tbl = session.table("u_datapump_jobset");
		Record rec1 = tbl.get("u_name", "junit-location-load");
		assertNotNull(rec1);
		Key key = rec1.getKey();
		DateTime date1 = rec1.getDateTime("u_run_start");
		tbl.update(key, new FieldValues().set("u_status", "pending"));
		tbl.update(key, new FieldValues().set("u_status", "running"));
		Record rec2 = tbl.get(key);
		DateTime date2 = rec2.getDateTime("u_run_start");
		logger.info("Date1=" + date1);
		logger.info("Date2=" + date2);
		assertTrue(date2.compareTo(date1) > 0);
	}

	@After
	public void tearDownAfter() throws SQLException {
		DB.rollback();
	}

}
