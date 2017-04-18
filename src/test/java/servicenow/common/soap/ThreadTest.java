package servicenow.common.soap;

import static org.junit.Assert.*;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Test;
import org.slf4j.Logger;

import servicenow.common.soap.DateTime;
import servicenow.common.soap.InvalidTableNameException;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableReader;

public class ThreadTest {

	Logger logger = AllTests.junitLogger(ThreadTest.class);
	
	static int NTHREADS = 10;
	
	DateTime getMonth(int i) throws ParseException {
		assertTrue(i >= 0);
		assertTrue(i < 12);
		String mm = Integer.toString(i + 1);
		if (mm.length() < 2) mm = "0" + mm;
		String d = "2015-" + mm + "-01 00:00:00";
		return new DateTime(d);
	}
	
	@Test
	public void test() throws Exception {
		final Session session = AllTests.getSession();
		Table table = session.table("change_request");
		table.setChunkSize(100);
		class ReaderThread extends Thread {
			Table table;
			QueryFilter filter;
			ReaderThread(Table table, QueryFilter filter) 
					throws InvalidTableNameException, IOException {
				this.table = table;
				this.filter = filter;
				logger.info("thread: " + filter.toString());
			}
			public void run() {
				int count = 0;
				TableReader reader;
				try {
					reader = table.reader(filter);
					while (reader.hasNext()) {
						RecordList chunk = reader.next();
						logger.info(chunk.size() + " records read");
						count += chunk.size();
					}
				} catch (IOException e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
				logger.info(count + " records read");
			}
			
		}
		ReaderThread threads[] = new ReaderThread[NTHREADS];
		for (int i = 0; i < NTHREADS; ++i) {
			DateTime d0 = getMonth(i);
			DateTime d1 = getMonth(i + 1);
			QueryFilter filter = new QueryFilter().addCreatedFilter(d0, d1);
			threads[i] = new ReaderThread(table, filter);
		}
		for (Thread t : threads) t.start();
		for (Thread t : threads) t.join(); 
		logger.info("All threads completed");
	}

}
