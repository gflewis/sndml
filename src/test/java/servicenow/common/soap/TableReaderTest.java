package servicenow.common.soap;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import org.slf4j.Logger;

import servicenow.common.soap.DateTime;
import servicenow.common.soap.KeyList;
import servicenow.common.soap.Record;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableReader;

import org.junit.BeforeClass;
import org.junit.Test;

public class TableReaderTest {

	static Session session;
	static Logger log = AllTests.junitLogger(TableReaderTest.class);
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		session = AllTests.getSession();
	}

	@Test
	public void testBasicReader() throws Exception {
		// chunksize must be smaller than the number of rows in the table
		final String tablename = "sys_user_group";
		final int chunksize = 7;
		Table tbl = session.table(tablename);
		tbl.setChunkSize(7);
		TableReader reader = tbl.reader();
		RecordList recs = reader.nextChunk();
		assertEquals(chunksize, recs.size());
		assertTrue(reader.hasNext());
	}

	@Test
	public void testGetAllRecords() throws Exception {
		final String tablename = "cmn_department";
		Table tbl = session.table(tablename);
		tbl.setChunkSize(50);
		KeyList keys = tbl.getKeys();
		RecordList recs = tbl.reader().getAllRecords();
		assertEquals(keys.size(), recs.size());
	}
	
	@Test
	public void testSortAscending() throws Exception {
		Table table = session.table("sys_user_group");
		table.setChunkSize(13);
		TableReader reader = table.reader().sortByUpdateDate();
		RecordList recs = reader.nextChunk();
		Record first = null;
		Record last = null;
		for (Record next : recs) {
			log.info("Ascending: " + next.getUpdatedTimestamp());
			if (first == null) {
				first = next;
			}
			else {
				DateTime d1 = last.getUpdatedTimestamp();
				DateTime d2 = next.getUpdatedTimestamp();
				assertTrue(d1.compareTo(d2) <= 0);
			}
			last = next;
		}
		DateTime d1 = first.getUpdatedTimestamp();
		DateTime d2 = last.getUpdatedTimestamp();
		assertTrue(d1.compareTo(d2) < 0);
	}

	@Test
	public void testSortDescending() throws Exception {
		TableReader reader = session.table("sys_user_group").reader();
		reader.sortDescending("sys_updated_on");
		RecordList recs = reader.nextChunk();
		Record first = null;
		Record last = null;
		for (Record next : recs) {
			log.info("Descending: " + next.getUpdatedTimestamp());
			if (first == null) {
				first = next;
			}
			else {
				DateTime d1 = last.getUpdatedTimestamp();
				DateTime d2 = next.getUpdatedTimestamp();
				assertTrue(d1.compareTo(d2) >= 0);
			}
			last = next;
		}
		DateTime d1 = first.getUpdatedTimestamp();
		DateTime d2 = last.getUpdatedTimestamp();
		assertTrue(d1.compareTo(d2) >= 0);
		assumeTrue(d1.compareTo(d2) > 0);
	}
	
}
