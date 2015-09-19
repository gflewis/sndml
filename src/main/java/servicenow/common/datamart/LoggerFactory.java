package servicenow.common.datamart;

import org.slf4j.Logger;

public class LoggerFactory {

	static final String loggerPrefix = "Datamart.";	
	
	public static Logger getLogger(@SuppressWarnings("rawtypes") Class c) {
		return getLogger(c, null);
	}
	
	public static org.slf4j.Logger getLogger(
			@SuppressWarnings("rawtypes") Class c, String tablename) {
		String loggername = loggerPrefix + c.getSimpleName();
		if (tablename != null) loggername = loggername + "." + tablename;
		return org.slf4j.LoggerFactory.getLogger(loggername);
	}
		
}
