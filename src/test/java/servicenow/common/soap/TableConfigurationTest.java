package servicenow.common.soap;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Test;

import servicenow.common.soap.SessionConfiguration;
import servicenow.common.soap.TableConfiguration;

public class TableConfigurationTest {

	@Test
	public void testConfiguration() {
		String tablename = "cmdb_ci_service";
		Properties props = new Properties();
		props.put("servicenow.limit",  "17");
		props.put("servicenow." + tablename + ".usekeys", "true");
		props.put("servicenow.othertable.limit", "18");
		props.put("servicenow." + tablename + ".interval", 300);
		TableConfiguration conf = new TableConfiguration(props, tablename);
		assertEquals(17, conf.getInt("limit"));
		assertEquals(true, conf.getBoolean("usekeys"));
		assertEquals(300, conf.getInt("interval"));
		assertEquals(250, conf.getInt("delay", 250));
	}

	@Test (expected = Exception.class)
	public void testConfigException() {
		Properties props = new Properties();
		props.put("servicenow.usekeys",  "fallllse");
		SessionConfiguration conf = new SessionConfiguration(props);
		conf.getBoolean("usekeys");
		fail();
	}
}
