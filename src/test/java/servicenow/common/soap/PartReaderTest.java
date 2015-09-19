package servicenow.common.soap;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import servicenow.common.soap.PartitionedTableReader;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;


public class PartReaderTest {

	Session session;
	static Logger logger = AllTests.junitLogger(PartReaderTest.class);
	
	@Before
	public void setUp() throws Exception {
		session = AllTests.getSession();
	}

	@Test
	public void testGetAllServers() throws Exception {
		Table server = session.table("cmdb_ci_server");
		int countAll = server.getCount();
		assertTrue(countAll > 100);
		PartitionedTableReader reader = new PartitionedTableReader(server, "sys_class_name");
		RecordList allRecs = reader.getAllRecords();
		assertEquals(countAll, allRecs.size());
	}

	@Test
	public void testGetOperationalServers() throws Exception {
		Table server = session.table("cmdb_ci_server");
		QueryFilter filter = new QueryFilter("operational_status", "1");
		int countAll = server.getCount(filter);
		assertTrue(countAll > 100);
		PartitionedTableReader reader = new PartitionedTableReader(server, "sys_class_name");
		reader.addFilter(new QueryFilter("operational_status", "1"));
		RecordList allRecs = reader.getAllRecords();
		assertEquals(countAll, allRecs.size());
	}
	
	@Test
	public void testGetEmptyList() throws Exception {
		Table server = session.table("cmdb_ci_server");
		QueryFilter filter = new QueryFilter("sys_class_name", "blah_blah");
		int countAll = server.getCount(filter);
		assertEquals(0, countAll);
		PartitionedTableReader reader = new PartitionedTableReader(server, "sys_class_name");
		reader.addFilter(filter);
		RecordList allRecs = reader.getAllRecords();
		assertEquals(countAll, allRecs.size());		
	}
}
