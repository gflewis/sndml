package servicenow.common.datamart;

import static org.junit.Assert.*;

import org.junit.*;

import java.io.IOException;

import org.slf4j.Logger;

import servicenow.common.soap.DateTime;
import servicenow.common.soap.FieldValues;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.Record;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableReader;

// import servicenow.common.soap.*;

public class JunitBackDateRefresh {

	static Logger logger = AllTests.getLogger(JunitBackDateRefresh.class);
	
	@Before
	public void setUpBefore() throws IOException  {
		AllTests.initialize();
	}
	
	@Test
	public void test() throws Exception {
		DateTime runStart = DateTime.now().subtractSeconds(120);
		Session session = AllTests.getSession();
		Table suiteTable = session.table("u_datapump_jobset");
		Table jobTable = session.table("u_datapump_job");
		Record suite1 = suiteTable.get("u_name", "junit-location-refresh");
		FieldValues values = new FieldValues();
		values.set("u_status", "pending");
		values.set("u_run_start", runStart);
		suiteTable.update(suite1.getKey(), values);
		Record suite2 = suiteTable.get(suite1.getKey());
		assertEquals(runStart, suite2.getDateTime("u_run_start"));
		QueryFilter filter = new QueryFilter("u_jobset", suite1.getKey().toString());
		TableReader reader = jobTable.petitReader(filter);
		RecordList jobrecs = reader.getAllRecords();
		assertTrue(jobrecs.size() > 0);
		for (Record jobrec : jobrecs) {
			assertNull(jobrec.getDateTime("u_interval_start"));
			assertEquals(runStart, jobrec.getDateTime("u_interval_end"));
		}
	}

}
