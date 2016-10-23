package servicenow.common.datamart;

import static org.junit.Assert.*;

import org.junit.*;

import java.sql.SQLException;

import org.slf4j.Logger;

import servicenow.common.datamart.CommandScript;
import servicenow.common.datamart.Loader;
import servicenow.common.datamart.Status;
import servicenow.common.datamart.SuiteModel;
import servicenow.common.soap.DateTime;

public class PollingTestBack {
	
	static Loader loader;
	static PollingThread pollingThread;
	static Logger logger = AllTests.getLogger(PollingTestBack.class);
	
	class PollingThread extends Thread {
		public void run() {
			try {
				loader.runToComplete();
			} catch (InterruptedException e) {
				logger.info("PollingThread terminated");
			} catch (Exception e) {
				fail();
				e.printStackTrace();
			}
		}		
	}
	
	@Before
	public void setUpBefore() throws Exception {
		AllTests.initialize();
		DB.sqlDeleteJupiter();
		SN.snDeleteJupiter();
		// SignalMonitor.clearSignal();
		DateTime start = DateTime.now().subtractSeconds(600);
		loader = new Loader(AllTests.getProperties());
		String[] input = {"every 10 seconds", "refresh cmn_location since " + start};
		CommandScript buffer = new CommandScript(input);
		loader.loadBuffer(buffer);
	}

	@Test
	public void test() throws Exception {
		assertEquals(0, DB.sqlCountJupiter());
		SN.snInsertJupiter();
		AllTests.sleep(5);
		SuiteModel suite = loader.getSuite();
		assertTrue(suite.isPolling());
		assertEquals(10, suite.getFrequency());
		assertEquals(Status.QUEUED, suite.getStatus());
		pollingThread = new PollingThread();
		pollingThread.start();
		AllTests.sleep(5);
		assertEquals(1, DB.sqlCountJupiter());
		pollingThread.interrupt();
	}

	@After
	public void tearDownAfter() throws SQLException {
		logger.info("tearDownAfter");
		pollingThread.interrupt();
		DB.rollback();
	}
	
}
