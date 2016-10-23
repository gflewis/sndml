package servicenow.common.datamart;

import static org.junit.Assert.*;

import org.junit.*;
import org.slf4j.Logger;

import servicenow.common.datamart.Loader;
import servicenow.common.datamart.Status;
import servicenow.common.datamart.SuiteModel;
import servicenow.common.soap.DateTime;
import servicenow.common.soap.FieldValues;
import servicenow.common.soap.Key;
import servicenow.common.soap.Record;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;

import java.sql.SQLException;

public class JunitPollingRunBack {

	static String classname = JunitPollingRunBack.class.getSimpleName();
	static Logger logger = AllTests.getLogger(JunitPollingRunBack.class);
	Integer pollInterval = new Integer(12);

	static String suiteName = "junit-location-refresh";
	static String jobName1 = "junit-location-refresh";
	static Loader loader;
	static LoaderThread loaderThread;
	
	class LoaderThread extends Thread {
		public void run() {
			try {
				loader.runToComplete();
			} catch (InterruptedException e) {
				logger.info("LoaderThread terminated");
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				fail();
			}
		}		
	}
	
	@Before
	public void setUpBefore() throws Exception {
		AllTests.initialize();
		DB.sqlDeleteJupiter();
		DB.commit();
		SN.snDeleteJupiter();
		System.gc();
	}
		
	@Test
	public void test() throws Exception {
		AllTests.banner(logger, classname,  "Begin");
		assertEquals(0, DB.sqlCountJupiter());
		assertEquals(0, SN.snCountJupiter());
		
		DateTime runStart = DateTime.now().subtractSeconds(120);
		Session session = AllTests.getSession();
		Table suiteTable = session.table("u_datapump_jobset");
		Record suiteRec = suiteTable.get("u_name", suiteName);
		Key suiteKey = suiteRec.getKey();
		FieldValues statusPendingValues = new FieldValues();
		statusPendingValues.set("u_schedule", "polling");
		statusPendingValues.set("u_status", "pending");
		statusPendingValues.setDuration("u_frequency",  pollInterval);
		statusPendingValues.set("u_run_start", runStart);
		statusPendingValues.setNull("u_next_run_start");
		suiteTable.update(suiteKey, statusPendingValues);
		suiteRec = suiteTable.get(suiteKey);
		assertEquals(pollInterval, suiteRec.getDuration("u_frequency"));
		assertEquals(runStart, suiteRec.getDateTime("u_run_start"));
		Table jobTable = session.table("u_datapump_job");		
		Record jobRec1 = jobTable.get("u_name", jobName1);
		DateTime intervalStart = jobRec1.getDate("u_interval_start");
		DateTime intervalEnd = jobRec1.getDate("u_interval_end");
		assertNull(intervalStart);
		assertEquals(runStart, intervalEnd);		
		
		//
		// Insert a record
		//
		AllTests.banner(logger, classname, "Insert record");
		SN.snInsertJupiter("ServiceNow Support");
		assertEquals(1, SN.snCountJupiter());
		assertEquals(0, DB.sqlCountJupiter());

		loader = AllTests.newLoader();
		loader.loadSuite(suiteName);
		SuiteModel suite = loader.getSuite();
		suite.setStatus(Status.READY);

		AllTests.banner(logger, classname, "Start LoaderThread");
		loaderThread = new LoaderThread();
		loaderThread.start();
		AllTests.sleep(pollInterval * 2 / 3);		
		assertEquals(1, DB.sqlCountJupiter());
		assertEquals(1, SN.snCountJupiter());
		//
		// Delete the record
		//
		AllTests.banner(logger, classname, "Delete record");
		SN.snDeleteJupiter();
		assertEquals(0, SN.snCountJupiter());
		assertEquals(1, DB.sqlCountJupiter());
		AllTests.sleep(pollInterval);
		assertEquals(0, SN.snCountJupiter());		
		assertEquals(0, DB.sqlCountJupiter());
		AllTests.banner(logger, classname, "End");
	}

	@After
	public void tearDownAfter() throws SQLException {
		logger.info("tearDownAfter");
		loaderThread.interrupt();
		DB.rollback();
	}
	
}
