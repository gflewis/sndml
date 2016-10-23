package servicenow.common.datamart;

import static org.junit.Assert.*;
import static org.junit.Assume.*;
import org.junit.*;

import servicenow.common.datamart.PersistentJob;
import servicenow.common.datamart.PersistentSuite;
import servicenow.common.soap.InvalidTableNameException;

public class GetJobByNameTest {

	@Before
	public void setUp() throws Exception {
		AllTests.initialize();
	}

	@Test
	public void test() throws Exception {
		try {
			PersistentSuite suite = AllTests.loadJunitSuite("junit-location-load");
			PersistentJob job = suite.getJobByName("junit-location-load");
			assertNotNull(job);
		}
		catch (InvalidTableNameException e) {
			assumeNoException(e);
		}
		
	}

}
