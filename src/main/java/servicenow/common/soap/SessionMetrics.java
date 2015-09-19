package servicenow.common.soap;

import java.util.*;
import org.slf4j.Logger;

/**
 * Contains metrics (records read, updated, etc.) for each operation.
 *
 */
public class SessionMetrics {

	TreeMap<String,Integer> metrics = new TreeMap<String,Integer>();

	public synchronized void increment(String tablename, String methodname) {
		String name = tablename + "." + methodname;
		Integer m = metrics.get(name);
		if (m == null) {
			metrics.put(name, new Integer(1));
		}
		else {
			metrics.put(name, new Integer(m.intValue() + 1));
		};
	}
	
	public void report(Logger logger) {
		String[] keys = metrics.keySet().toArray(new String[0]);
		Arrays.sort(keys);
		for (int i = 0; i < keys.length; ++i) {
			String key = keys[i];
			Integer metric = metrics.get(key);
			logger.info("metric " + key + " " + metric.toString());
		}
	}
}

