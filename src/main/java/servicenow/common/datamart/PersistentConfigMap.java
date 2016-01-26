package servicenow.common.datamart;

import java.io.IOException;
import java.util.MissingResourceException;
import java.util.Properties;

/**
 * The SNDML configuration is stored in ServiceNow tables,
 * but we are uncertain as to the table and column names.
 * This class enables us to map those names.
 */
public class PersistentConfigMap {

	static String propsResource = "persistent.properties";
	static Properties props;
	
	public PersistentConfigMap() {
		if (props == null) {
			props = new Properties();
			try {
				props.load(ClassLoader.getSystemResourceAsStream(propsResource));
			}
			catch (IOException e) {
				throw new MissingResourceException(
					e.getMessage(), PersistentConfigMap.class.getName(), propsResource);
			}
		}
	}

	/**
	 * Return the name of an object
	 * @throws IOException 
	 */
	String getName(String prop) {
		String result = props.getProperty(prop);
		if (result == null)
			throw new MissingResourceException(
				"property not found: " + prop, 
				PersistentConfigMap.class.getName(), prop);
		return result;
	}	
		
}
