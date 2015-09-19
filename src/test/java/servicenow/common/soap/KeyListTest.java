package servicenow.common.soap;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import servicenow.common.soap.BasicTableReader;
import servicenow.common.soap.Key;
import servicenow.common.soap.KeyList;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;

public class KeyListTest {

	static Session session;
	static Table tbl;
	static KeyList keys;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		session = AllTests.getSession();
		tbl = session.table("sys_user_group");
		keys = tbl.getKeys();
		tbl.setChunkSize(30);
	}

	@Test
	public void testReadTable() throws Exception {
		RecordList recs2 = tbl.getRecords(keys, 0, keys.size());
		assertEquals(recs2.size(), keys.size());
		RecordList recs1 = tbl.reader().getAllRecords();
		assertEquals(recs2.size(), recs1.size());
	}

	@Test
	public void testGetSubset() throws Exception {
		RecordList recs = tbl.getRecords(keys, 0, 15);
		assertEquals(recs.size(), 15);		
	}

	@Test
	public void testGetBigKeys() throws Exception {
		Table big = session.table("cmdb_ci_ip_address");
		BasicTableReader reader = big.reader();
		KeyList keys = reader.getKeys();
		assertTrue(keys.size() > 40000);
		HashMap<Key,Boolean> hash = new HashMap<Key,Boolean>();
		for (Key key : keys) {
			assertNull(hash.get(key));
			assertTrue(key.isValidGUID());
			hash.put(key, new Boolean(true));
		}
		assertEquals(keys.size(), hash.size());
	}
		
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

}
