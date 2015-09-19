package servicenow.common.soap;

import java.util.Properties;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;

import servicenow.common.soap.SessionConfiguration;
import servicenow.common.soap.Table;

/**
 * Configuration information for a {@link Table}.
 * <p/>
 * Strips the "servicenow." or "servicenow.tablename." prefix from properties.
 * <p/>
 * Based on org.apache.commons.configuration.BaseConfiguration.
 * @see <a href="http://commons.apache.org/proper/commons-configuration/apidocs/org/apache/commons/configuration/BaseConfiguration.html"
 * >http://commons.apache.org/proper/commons-configuration/apidocs/org/apache/commons/configuration/</a>
 *
 */
public class TableConfiguration extends BaseConfiguration {

	public TableConfiguration(Configuration base, String tablename) {
		super();
		copy(base);
		copy(base.subset(tablename));
	}
	
	public TableConfiguration(Properties props, String tablename) {
		this(new SessionConfiguration(props), tablename);
	}
}
