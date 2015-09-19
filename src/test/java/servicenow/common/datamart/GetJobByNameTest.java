package servicenow.common.datamart;

import static org.junit.Assert.*;

import org.junit.*;

import servicenow.common.datamart.PersistentJob;
import servicenow.common.datamart.PersistentSuite;

public class GetJobByNameTest {

	@Before
	public void setUp() throws Exception {
		AllTests.initialize();
	}

	@Test
	public void test() throws Exception {
		PersistentSuite suite = AllTests.loadJunitSuite("junit-location-load");
		PersistentJob job = suite.getJobByName("junit-location-load");
		assertNotNull(job);
	}

}
