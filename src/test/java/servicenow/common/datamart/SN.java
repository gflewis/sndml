package servicenow.common.datamart;

import java.io.IOException;

import org.jdom2.JDOMException;
import org.slf4j.Logger;

import servicenow.common.datamart.ResourceManager;
import servicenow.common.soap.FieldValues;
import servicenow.common.soap.Key;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.Record;
import servicenow.common.soap.RecordIterator;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;

public class SN {

	static boolean initialized = false;
	static Session session;
	static Table location;
	static Logger log = AllTests.getLogger(SN.class);
	
	static void initialize() throws IOException, JDOMException {		
		if (initialized) return;
		AllTests.initialize();
		session = ResourceManager.getMainSession();
		location = session.table("cmn_location");
		initialized = true;
	}
	
	static Session getSession() throws IOException, JDOMException {
		initialize();
		return session;
	}
	
	static void snDeleteJupiter() throws Exception {
		initialize();
		RecordList recs = location.getRecords("name", "Jupiter");
		int count = 0;
		RecordIterator iter = recs.iterator();
		while (iter.hasNext()) {
			Record rec = iter.next();
			Key key = rec.getKey();
			log.debug("deleting " + key);
			location.deleteRecord(key);
			count += 1;
		}
		log.debug(count + " records deleted");
	}
	
	static Key snInsertJupiter(String support_group) throws Exception {
		initialize();
		FieldValues initial_values = 
			new FieldValues().
				set("name", "Jupiter").
				set("u_support_group", support_group);
		return location.insert(initial_values).getSysId();
	}
	
	static Key snInsertJupiter() throws Exception {
		initialize();
		String group = AllTests.junitProperty("some_group_name");
		return snInsertJupiter(group);
	}
	
	static void snUpdateJupiter(Key key, String support_group) throws Exception {
		initialize();
		FieldValues update_values = 
			new FieldValues().
				set("u_support_group", support_group);
		location.update(key, update_values);		
	}
	
	static int snCountJupiter() throws Exception {
		initialize();
		QueryFilter filter = new QueryFilter("name", "Jupiter");
		return location.getCount(filter);
	}
	
}
