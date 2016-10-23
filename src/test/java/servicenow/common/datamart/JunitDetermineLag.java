package servicenow.common.datamart;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.sql.SQLException;
import java.util.Date;

import org.slf4j.Logger;

import servicenow.common.soap.DateTime;
import servicenow.common.soap.FieldValues;
import servicenow.common.soap.Key;
import servicenow.common.soap.Record;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JunitDetermineLag {

	static Logger logger = AllTests.getLogger(JunitDetermineLag.class);	

	@Before
	public void setUpBefore() throws Exception {
		AllTests.initialize();
	}
	
	@Test
	public void test() throws Exception {
		Session session = AllTests.getSession();
		assumeTrue(session.tableExists("u_datapump_jobset"));
		Table table = session.table("u_datapump_jobset");
		Record rec1 = table.get("u_name", "junit-location-load");
		Key key1 = rec1.getKey();
		assertNotNull(rec1);
		FieldValues statusReady   = new FieldValues().set("u_status", "pending");
		FieldValues statusRunning = new FieldValues().set("u_status", "running");
		table.update(key1, statusReady);		
		DateTime today = DateTime.today();
		Date before = new Date();
		logger.info("setting status=running");
		table.update(key1, statusRunning);
		Record rec2 = table.get(key1);		
		assertEquals("running", rec2.getField("u_status"));
		DateTime updated = rec2.getDateTime("sys_updated_on");
		assertTrue(today.compareTo(updated) < 0);
		Date after = new Date();
		long lagSeconds = 
			(before.getTime() - updated.toDate().getTime()) / 1000L;
		logger.info("updated=" + updated.toString());
		logger.info("before =" + before);
		logger.info("updated=" + updated.toDate().toString());
		logger.info("after  =" + after);
		logger.info("lag    =" + lagSeconds);
		assertTrue(lagSeconds > -2 && lagSeconds < 2);
	}
	
	@After
	public void tearDown() throws SQLException {
		DB.rollback();
	}

}
