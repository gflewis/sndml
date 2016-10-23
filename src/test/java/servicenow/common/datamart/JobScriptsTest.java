package servicenow.common.datamart;

import static org.junit.Assert.*;

import java.sql.SQLException;

import org.slf4j.Logger;

import servicenow.common.datamart.JobModel;
import servicenow.common.datamart.LoadMethod;
import servicenow.common.datamart.Status;
import servicenow.common.datamart.SuiteModel;
import servicenow.common.datamart.SuiteParseException;
import servicenow.common.soap.DateTime;

import org.junit.*;

public class JobScriptsTest {

	static Logger log = AllTests.getLogger(JobScriptsTest.class);
	
	@Before
	public void setUpBefore() throws Exception {
		AllTests.initialize();
	}

	static SuiteModel parse(String text) throws Exception {
		log.info("i:" + text);
		SuiteModel suite;
		try {
			suite = AllTests.newSuite(text);
		} 
		catch (SuiteParseException e) {
			log.info("e:" + e.getMessage());
			throw e;
		}
		log.info("o:" + suite.getDescription());
		return suite;
	}
		
	@Test (expected = Exception.class)
	public void testBadCommand() throws Exception {
		parse("regress cmn_department");
		fail();
	}
	
	@Test (expected = SuiteParseException.class)
	public void testSpuriousOption() throws Exception {
		parse("load cmn_department okay");
		fail();
	}
	
	@Test
	public void testMultipleJobs() throws Exception {
		String[] mjobs = {
			"load cmn_department",
			"load cmn_location",
			"load sys_user"};
		SuiteModel suite = AllTests.newSuite(mjobs);
		assertEquals(3, suite.numJobs());
	}
	
	@Test 
	public void testGoodRefresh() throws Exception {
		SuiteModel suite = parse("load cmn_department compare-timestamps");
		String name = suite.getJobs().get(0).getName();
		assertEquals(name, "cmn_department");
	}
	
	@Test
	public void testDefaultMethod() throws Exception {
		SuiteModel suite = parse("load cmn_department");
		assertEquals(1, suite.numJobs());
		JobModel job = suite.getJobs().get(0);
		assertEquals(LoadMethod.UPDATE_INSERT, job.getMethod());
	}

	@Test
	public void testGoodDate() throws Exception {
		SuiteModel suite = 
			parse("load cmn_department from 2012-01-01 to 2012-01-01 15:00:00");
		assertEquals(1, suite.numJobs());
		JobModel job = suite.getJobs().get(0);
		assertEquals(new DateTime("2012-01-01 00:00:00"), job.getIntervalStart());
		assertEquals(new DateTime("2012-01-01 15:00:00"), job.getIntervalEnd());		
	}
	
	@Test (expected = SuiteParseException.class)
	public void testBadDate() throws Exception {
		parse("load cmn_department from 2012-7777");
		fail();
	}

	@Test (expected = SuiteParseException.class)
	public void testBadTime() throws Exception {
		parse("load cmn_department from 2012-01-01 00abc");
		fail();
	}

	@Test
	public void testNow() throws Exception {
		SuiteModel suite =  parse("load cmn_department from 2012-01-01 to now");
		assertNotNull(suite.getJobs().get(0));
	}
	
	@Test
	public void testWhereNotQuotes() throws Exception {
		SuiteModel suite = parse("load cmn_department where blah=blah^blah=blah");
		assertNotNull(suite.getJobs().get(0));
	}
	
	@Test 
	public void testWhereQuoted() throws Exception {
		SuiteModel suite =  parse(
			"load cmn_department where { blah=blah blah^foo=foo bar }");
		assertNotNull(suite.getJobs().get(0));
	}
	
	@Test
	public void testSort() throws Exception {
		SuiteModel suite = parse("load cmn_department");
		JobModel job = suite.getJobs().get(0);
		String sort = job.getSortField();
		assertEquals("sys_created_on", sort);
	}
	
	@Test
	public void testInitialStatus() throws Exception {
		SuiteModel suite = parse("load cmn_department");
		JobModel job = suite.getJobs().get(0);
		assertEquals(Status.QUEUED, job.getStatus());
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws SQLException {
		DB.rollback();
	}
	
}
