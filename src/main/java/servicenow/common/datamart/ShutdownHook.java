package servicenow.common.datamart;

import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import servicenow.common.datamart.DatamartConfiguration;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.ShutdownHook;

public class ShutdownHook extends Thread {

	final Logger logger = LoggerFactory.getLogger(ShutdownHook.class);
	final DatamartConfiguration config;
	final Thread mainThread; 
	final TimerTask dispenser;
	final ExecutorService workerPool;  // null if single threaded
	
	ShutdownHook(TimerTask dispenser, ExecutorService workers) {
		config = DatamartConfiguration.getDatamartConfiguration();
		this.mainThread = Thread.currentThread();
		this.dispenser = dispenser;
		this.workerPool = workers;
	}
		
	ShutdownHook() {
		this(null, null);
	}
	
	@Override
	public void run() {
		logger.info("ShutdownHook invoked");
		boolean terminated = false;
		if (dispenser != null) {
			dispenser.cancel();
		}
		if (workerPool == null) {
			// must be single threaded
			// interrupt the main thread
			mainThread.interrupt();
			terminated = true;
		}
		else {
			// send interrupt to all workers
			workerPool.shutdownNow();
			int waitSec = config.getInt("shutdown_seconds", 30);
			logger.info("Awaiting worker pool termination");
			try {
				terminated = workerPool.awaitTermination(waitSec, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		}
		if (!terminated) logger.warn("Some threads failed to terminate");
		logger.info("ShutdownHook complete");
	}

}
