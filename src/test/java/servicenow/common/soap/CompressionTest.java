package servicenow.common.soap;

import static org.junit.Assert.*;

import java.util.Properties;

import org.slf4j.Logger;

import servicenow.common.soap.BasicTableReader;
import servicenow.common.soap.KeyList;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;

import org.junit.Test;

public class CompressionTest {

	Logger logger = AllTests.junitLogger(CompressionTest.class);
	
	@Test
	public void testCompression() throws Exception {
		Properties baseprops = AllTests.getProperties();
		Properties props1 = (Properties) baseprops.clone();
		Properties props2 = (Properties) baseprops.clone();
		props1.setProperty("servicenow.compression", "false");
		props2.setProperty("servicenow.compression", "true");
		Session session1 = new Session(props1);
		Table table1 = session1.table("cmdb_ci_computer");
		logger.debug("Reading uncompressed");
		BasicTableReader reader1 = table1.reader();
		reader1.sortAscending("sys_id");
		KeyList keys1 = reader1.getKeys();
		logger.info("Uncompressed: " + keys1.size() + " keys");
		Session session2 = new Session(props2);
		Table table2 = session2.table("cmdb_ci_computer");
		logger.debug("Reading compressed");
		BasicTableReader reader2 = table2.reader();
		reader2.sortAscending("sys_id");
		KeyList keys2 = reader2.getKeys();
		logger.info("Compressed: " + keys2.size() + " keys");
		assertEquals(keys1.size(), keys2.size());
	}

}
