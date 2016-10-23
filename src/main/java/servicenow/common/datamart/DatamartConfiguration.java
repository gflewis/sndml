package servicenow.common.datamart;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.slf4j.Logger;

import servicenow.common.datamart.DatamartConfiguration;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.ResourceException;
import servicenow.common.soap.SessionConfiguration;

public class DatamartConfiguration extends BaseConfiguration {

	public static String PREFIX = "datamart";

	// Default properties file name
	static File propfile = new File("sndm.properties");
	
	static Properties properties = null;
	
	static final Logger logger = LoggerFactory.getLogger(DatamartConfiguration.class);
	
	DatamartConfiguration(Properties props) {
		super();
		assert !props.isEmpty();
		copy(ConfigurationConverter.getConfiguration(props).subset(PREFIX));
		assert !this.isEmpty();
	}
		
	String getRequiredString(String propname) throws IllegalArgumentException {
		String result = null;
		try {
			result = getString(propname);
		}
		catch (NoSuchElementException e) { }
		if (result == null)
			throw new IllegalArgumentException(
				"missing property: " + PREFIX + "." + propname);
		return result;
	}

	public void logTrace(Logger logger) {
		logger.trace("empty=" + this.isEmpty());
		Iterator<String> iter = getKeys();
		while (iter.hasNext()) {
			String name = iter.next();
			logger.trace(name + "=" + getProperty(name));				
		}
	}

	static File setPropFile(File newfile) {
		propfile = newfile;
		properties = null;
		return newfile;
	}

	public static Properties setProperties(Properties props) {
		properties = props;
		assert properties.size() > 0;
		return props;
	}
	
	public static Properties getProperties() 
			throws ResourceException {
		if (properties != null) return properties;
		properties = new Properties();
		logger.info("propfile=" + propfile);
		try {
			InputStream propstream = new FileInputStream(propfile);
			properties.load(propstream);
		}
		catch (IOException e) {
			throw new ResourceException(e);
		}
		assert properties.size() > 0;
		if (logger.isTraceEnabled()) {
			TreeSet<String> names = new TreeSet<String>(properties.stringPropertyNames());
			for (String name : names) {
				logger.trace(name + "=" + properties.getProperty(name));				
			}
		}
		logger.debug("getProperties size=" + properties.size());
		return properties;
	}

	public static DatamartConfiguration getDatamartConfiguration() 
			throws ResourceException {
		Properties props = getProperties();
		assert props.size() > 0;
		return new DatamartConfiguration(props);
	}
	
	public static SessionConfiguration getSessionConfiguration() 
			throws ResourceException {
		SessionConfiguration config = new SessionConfiguration(getProperties());
		logger.debug("url=" + config.getString("url", null));
		logger.debug("username=" + config.getString("username", null));
		return config;
	}
	
}
