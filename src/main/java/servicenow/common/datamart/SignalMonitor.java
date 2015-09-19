package servicenow.common.datamart;

import java.io.File;
import java.io.IOException;
import java.util.TimerTask;

import org.slf4j.Logger;

import servicenow.common.datamart.Daemon;
import servicenow.common.datamart.DatamartConfiguration;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.SignalMonitor;

/**
 * Used to sleep and check for the existence of a signal file,
 * which indicates that the process is to be cancelled.
 * It the signal file is detected then an InterruptedException is thrown.
 * 
 * @author Giles Lewis
 *
 */
@Deprecated
public class SignalMonitor extends TimerTask {

	private static DatamartConfiguration config = null;
	private static String filename = null;
	private static File sigfile = null;
	private static boolean shutdownflag = false;
	private static Logger logger = LoggerFactory.getLogger(SignalMonitor.class);
	
	public SignalMonitor() {
	}
	
	@Override
	public void run() {
		if (signalDetected()) {				
			Daemon.shutdown();
			this.cancel();
		}		
	}
	
	private static void init() {
		config = DatamartConfiguration.getDatamartConfiguration();
		filename = config.getString("signal_file");
		if (filename == null) {
			sigfile = null;
			logger.warn("signal_file not specified");
		}
		else {
			sigfile = new File(filename);
			logger.info("signal_file: " + sigfile.getAbsolutePath());
		}
	}
	
	/**
	 * Shuts down all processes (including processes that are not part
	 * of this process tree) by creating a signal file. 
	 */
	static public void shutdownAllServers() throws IOException {
		if (config == null) init();
		logger.warn("Shutdown has been invoked");
		if (sigfile == null)
			throw new UnsupportedOperationException("signal_file not defined");
		logger.info("Creating signal_file: " + filename);
		sigfile.createNewFile();		
	}
	
	/**
	 * Delete the signal flag.
	 * @throws IOException
	 */
	static public void clearSignal() throws IOException {
		if (config == null) init();
		if (sigfile == null) {
			logger.info("clearSignal: ignored (signal_file not defined)");
			return;
		}
		else {
			logger.info("clearSignal: Removing " + filename);
			if (sigfile.exists()) {
				sigfile.delete();				
			}
		}
		shutdownflag = false;
	}
	
	/**
	 * Determine if shutdown has been requested.
	 * @return true if shutdown has been requested, otherwise false.
	 */
	static public boolean signalDetected() {
		if (config == null) init();
		if (shutdownflag) return true;
		if (sigfile == null) return false;
		if (sigfile.exists()) {
			logger.info("signal_file detected");
			shutdownflag = true;
			return true;
		}
		return false;
	}
				
}
