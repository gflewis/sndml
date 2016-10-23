package servicenow.common.datamart;

import static org.junit.Assert.*;

import org.junit.*;

import java.sql.SQLException;

import org.slf4j.Logger;

import servicenow.common.datamart.JobModel;
import servicenow.common.datamart.SuiteModel;
import servicenow.common.soap.DateTime;
import servicenow.common.soap.Table;

public class PruneTest {

	Logger logger = AllTests.getLogger(PruneTest.class);
	
	static SuiteModel load;
	static SuiteModel prune;
	
	@Before
	public void setUpBefore() throws Exception {
		AllTests.getConfiguration();
	}

	@Test
	public void test() throws Exception {
		String tablename = "cmn_location";
		Table location = AllTests.getSession().table(tablename);
		SN.snDeleteJupiter();
		int snBeforeInsert = location.getCount();
		logger.info("SN before insert=" + snBeforeInsert);
		SN.snInsertJupiter();		
		int snAfterInsert = location.getCount();
		logger.info("SN after  insert=" + snAfterInsert);
		AllTests.sleep(2);
		load = AllTests.newSuite("load cmn_location truncate");
		load.getController().runOnce();
		int dbBeforeDelete = DB.sqlCountTable(tablename);
		logger.info("DB before delete=" + dbBeforeDelete);
		assertTrue(dbBeforeDelete > 0);		
		assertEquals(snAfterInsert, dbBeforeDelete);
		DateTime start = DateTime.now();
		AllTests.sleep(2);
		SN.snDeleteJupiter();
		int snAfterDelete = location.getCount();
		logger.info("SN after  delete=" + snAfterDelete);
		AllTests.sleep(2);
		prune = AllTests.newSuite("prune cmn_location since " + start);
		JobModel model = prune.getJobs().get(0);
		logger.info("Interval Start=" + model.getIntervalStart());
		logger.info("Interval End  =" + model.getIntervalEnd());
		prune.getController().runOnce();
		int dbAfterDelete = DB.sqlCountTable(tablename);
		logger.info("DB before delete=" + dbBeforeDelete);
		logger.info("DB after  delete=" + dbAfterDelete);
		if (dbBeforeDelete == dbAfterDelete) {
			DB.printReport("select sys_id, name, sys_created_on from cmn_location");			
		}
		assertEquals(dbBeforeDelete - 1, dbAfterDelete);
	}

	@After
	public void tearDownAfter() throws SQLException {
		DB.rollback();
	}
	
}
