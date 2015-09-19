package servicenow.common.soap;

import org.slf4j.Logger;

import servicenow.common.soap.ServiceNowException;

@SuppressWarnings("serial")
public class ServiceNowException extends Exception {
	
	public ServiceNowException(String message) {
		super(message);
	}

	protected ServiceNowException() {
		super();
	}

	/**
	 * Static function to print an error to the log file and throw an exception.
	 * This function will not return.
	 * 
	 * @param logger
	 * @param message
	 * @throws ServiceNowException
	 */
	public static void logFatal(Logger logger, String message) 
			throws ServiceNowException {
		ServiceNowException e = new ServiceNowException(message);
		logger.error(message, e);
		throw e;
	}
	
	public static void logFatal(Logger logger)
			throws ServiceNowException {
		ServiceNowException e = new ServiceNowException();
		String msg = e.getClass().getName();
		logger.error(msg, e);
		throw e;
	}
	
}
