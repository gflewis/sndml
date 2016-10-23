package servicenow.common.datamart;

import java.io.IOException;

import org.slf4j.Logger;

import servicenow.common.datamart.DatamartConfiguration;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.NextRunStart;
import servicenow.common.datamart.SignalMonitor;
import servicenow.common.soap.DateTime;

@Deprecated
public class NextRunStart {
	
	private static DatamartConfiguration config = null;
	
	private static DateTime nextRunStart;

	static final Logger logger = LoggerFactory.getLogger(NextRunStart.class);
			
	private static void init() {
		if (config == null) {
			config = DatamartConfiguration.getDatamartConfiguration();
		}
		assert config != null;
	}
	
	static synchronized void reset() {
		if (config == null) init();
		int intervalSec = config.getInt("interval_seconds", 20);
		DateTime now = getLocalTime();
		nextRunStart = now.addSeconds(intervalSec);
		logger.debug("reset " + nextRunStart + " (" + now + " + " + intervalSec + ")");
	}
	
	static synchronized void setNext(DateTime suiteNextRun) {
		if (suiteNextRun.compareTo(nextRunStart) < 0)
			nextRunStart = suiteNextRun;
		logger.debug("setNext " + nextRunStart);
	}
	
	static synchronized DateTime getNextRun() {
		return nextRunStart;
	}
		
	static void sleepUntilNext() 
			throws IOException, InterruptedException {
		logger.info("sleep from now=" + DateTime.now() + " until=" + nextRunStart);
		while (getNextRun().compareTo(getLocalTime()) >= 0) {
			Thread.sleep(1000);
			if (SignalMonitor.signalDetected()) return;
		}		
	}
			
	private static DateTime getLocalTime() {
		assert config != null;
		int lagSeconds = config.getInt("lag_seconds", 0);		
		DateTime now = DateTime.now();
		if (lagSeconds != 0) now = now.subtractSeconds(lagSeconds);
		return now;
	}
		
}
