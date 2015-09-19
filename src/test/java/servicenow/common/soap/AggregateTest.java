package servicenow.common.soap;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.*;
import org.slf4j.Logger;

import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;

public class AggregateTest {

	static Logger logger = AllTests.junitLogger(AggregateTest.class);
	
	@Test
	public void testCount() throws Exception {
		Session session = AllTests.getSession();
		Table location = session.table("cmn_location");
		int countAll = location.getCount();
		logger.info("countAll=" + countAll);
		assertTrue(countAll > 10);
		QueryFilter filterActive = new QueryFilter("u_inactive", "false");
		QueryFilter filterInactive = new QueryFilter("u_inactive", "true");
		int countActive = location.getCount(filterActive);
		logger.info("countActive=" + countActive);
		assertTrue(countActive > 0);
		assertTrue(countActive < countAll);
		int countInactive = location.getCount(filterInactive);
		logger.info("countInactive=" + countInactive);
		assertTrue(countInactive > 0);		
		assertEquals(countAll, countInactive + countActive);
	}
	
	@Test
	public void testCountByClass() throws Exception {
		Session session = AllTests.getSession();
		Table service = session.table("cmdb_ci_service");
		int allServices = service.getCount();
		Map<String,Integer> allRows = service.getCount("sys_class_name");
		logger.info("rows=" + allRows.size());
		int allTotal = 0;
		for (String tbl : allRows.keySet()) {
			int count = allRows.get(tbl);
			logger.info(tbl + "=" + count);
			assertTrue(count > 0);
			allTotal += count;
		}
		logger.info("total=" + allTotal);
		assertEquals(allServices, allTotal);
		QueryFilter activeFilter = new QueryFilter("operational_status=1");
		int activeServices = service.getCount(activeFilter);
		assertTrue(activeServices < allServices);
		Map<String,Integer> activeRows = service.getCount("sys_class_name", activeFilter);
		int activeTotal = 0;
		for (String tbl : activeRows.keySet()) {
			int count = activeRows.get(tbl);
			assertTrue(count <= allRows.get(tbl));
			activeTotal += count;
		}
		logger.info("active=" + activeServices);
		assertEquals(activeServices, activeTotal);
	}

}
