package servicenow.common.soap;

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.Logger;

import servicenow.common.soap.FieldValues;
import servicenow.common.soap.Key;
import servicenow.common.soap.Record;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;

public class UpdateDurationTest {

	Logger logger = AllTests.junitLogger(UpdateDurationTest.class);
	
	@Test
	public void testUpdateDuration() throws Exception {
		String tablename = "u_datapump_job";
		String fieldname = "u_frequency";
		FieldValues values1 = new FieldValues();
		FieldValues values2 = new FieldValues();
		FieldValues valuesNull = new FieldValues();
		values1.setDuration(fieldname, 30);
		values2.setDuration(fieldname, 40);
		valuesNull.setNull(fieldname);
		Session session = AllTests.getSession();
		Table table = session.table(tablename);
		Record rec1 = table.get("u_name", "junit-location-prune");
		assertNotNull(rec1);
		Key key = rec1.getKey();
		logger.info("Frequency=" + rec1.getField(fieldname));
		table.update(key, values1);
		Record rec2 = table.get(key);
		logger.info("Frequency=" + rec2.getField(fieldname));
		assertEquals(30, rec2.getDuration(fieldname).intValue());
		table.update(key, values2);
		Record rec3 = table.get(key);
		logger.info("Frequency=" + rec3.getField(fieldname));
		assertEquals(40, rec3.getDuration(fieldname).intValue());
		table.update(key, valuesNull);
		Record rec4 = table.get(key);
		assertNull(rec4.getDuration(fieldname));
	}

}
