package servicenow.common.soap;

import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.slf4j.Logger;

import servicenow.common.soap.Session;


/**
 * Configuration information for a {@link Session}.
 * <p/>
 * Strips the "servicenow." prefix from properties.
 * <p/>
 * Based on org.apache.commons.configuration.BaseConfiguration.
 * @see <a href="http://commons.apache.org/proper/commons-configuration/apidocs/org/apache/commons/configuration/BaseConfiguration.html"
 * >http://commons.apache.org/proper/commons-configuration/apidocs/org/apache/commons/configuration/</a>
 *
 */
public class SessionConfiguration extends BaseConfiguration {

	public static String PREFIX = "servicenow";

	SessionConfiguration() {
		super();
	}
	
	SessionConfiguration(Configuration base) {
		super();
		assert !base.isEmpty();
		copy(base.subset(PREFIX));
		assert !this.isEmpty();
	}
	
	public SessionConfiguration(Properties props) {
		this(ConfigurationConverter.getConfiguration(props));
	}

	public void logTrace(Logger logger) {
		logger.trace("empty=" + this.isEmpty());
		Iterator<String> iter = getKeys();
		while (iter.hasNext()) {
			String name = iter.next();
			logger.trace(name + "=" + getProperty(name));				
		}
	}
	
}
