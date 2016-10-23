package servicenow.common.datamart;

import static org.junit.Assert.*;

import org.junit.*;

import java.sql.SQLException;

import org.slf4j.Logger;

import servicenow.common.datamart.JobOperation;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.PersistentJob;
import servicenow.common.datamart.PersistentSuite;
import servicenow.common.soap.DateTime;
import servicenow.common.soap.FieldValues;
import servicenow.common.soap.Key;
import servicenow.common.soap.Record;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;

public class JobModelTest {

	static Logger logger = LoggerFactory.getLogger(JobModelTest.class);
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		AllTests.initialize();
	}

	@Test
	public void testGoodOperation() throws Exception {
		PersistentSuite loadSuite    = AllTests.loadJunitSuite("junit-location-load");
		PersistentSuite refreshSuite = AllTests.loadJunitSuite("junit-location-refresh");
		PersistentJob loadJob      = loadSuite   .getJobByName("junit-location-load");
		PersistentJob refreshJob   = refreshSuite.getJobByName("junit-location-refresh");
		assertNotNull(loadJob);
		assertNotNull(refreshJob);
		JobOperation op = loadJob.getOperation();
		assertNotNull(op);
		assertEquals(JobOperation.LOAD, op);
		assertEquals("load", op.toString().toLowerCase());
		assertEquals("refresh", refreshJob.getOperation().toString().toLowerCase());
	}
	
	@Test
	public void testSetStatus1() throws Exception {
		Session session = AllTests.getSession();
		Table ujob = session.table("u_datapump_job");
		Record rec0 = ujob.get("u_name","junit-location-refresh");
		Key key = rec0.getKey();
		ujob.update(key, new FieldValues().
			setNull("u_interval_start").
			setNull("u_interval_end").
		    set("u_status", "pending"));
		// after initialization
		Record rec1 = ujob.get(key);
		assertNull(rec1.getDate("u_interval_start"));
		assertNull(rec1.getDate("u_interval_end"));
		// set status to running
		ujob.update(key, new FieldValues().set("u_status", "running"));
		Record rec2 = ujob.get(key);
		assertNotNull(rec2.getDate("u_interval_start"));
		assertNotNull(rec2.getDate("u_interval_end"));
		// set status to pending
		ujob.update(key, new FieldValues().set("u_status", "pending"));
	}

	@Test
	public void testStart() throws Exception {
		String suitename = "junit-location-refresh";
		String jobname = "junit-location-refresh";
		PersistentJob job = AllTests.loadJunitSuite(suitename).getJobByName(jobname);
		DateTime ago10 = DateTime.now().subtractSeconds(10 * 60);
		DateTime ago20 = DateTime.now().subtractSeconds(20 * 60);
		DateTime ago30 = DateTime.now().subtractSeconds(30 * 60);
		logger.info("ago10=" + ago10);
		logger.info("ago20=" + ago20);
		logger.info("ago30=" + ago30);
		Record rec = job.readRecord();
		Table ujob = rec.getTable();
		Key key = rec.getKey();
		ujob.update(key, new FieldValues().
			set("u_interval_start", ago30).
			set("u_interval_end", ago20));
		rec = job.readRecord();
		assertEquals(ago30.toString(), rec.getField("u_interval_start"));
		assertEquals(ago20.toString(), rec.getField("u_interval_end"));
	}
	
	@After
	public void tearDownAfter() throws SQLException {
		DB.rollback();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		DB.rollback();
	}


}
