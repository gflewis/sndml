package servicenow.common.datamart;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.junit.*;

import servicenow.common.datamart.Loader;


public class LoaderOptionsTest {

	static Logger log = AllTests.getLogger(LoaderOptionsTest.class);
	static Loader loader;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		loader = AllTests.newLoader();
	}
	
	@Test (expected = Exception.class)
	public void testBadOption() throws Exception {
		String[] args = { "-blah" };
		Loader.main(args);		
	}
	
	@Test
	public void testGoodScript() throws Exception {
		File goodfile = AllTests.tempFile("refresh_users.txt");
		FileUtils.writeStringToFile(goodfile,
			"refresh sys_user since today\n");
		loader.loadScriptFile(goodfile);
	}

	@Test (expected = FileNotFoundException.class)
	public void testBadScript() throws Exception {
		File badfile = new File("xxx/yyy/zzz.txt");
		loader.loadScriptFile(badfile);
	}
	
	@Test
	public void testParseFile() throws Exception {
		String[] args1 = { "-p", "myprops.properties", "-f", "/abc/def.ghi" };
		Properties options = Loader.parseOptions(args1);
		assertEquals("/abc/def.ghi", options.getProperty("scriptfilename"));
	}
	
	@Test (expected = Exception.class)
	public void testParseBadOption() throws Exception {
		String[] args1 = { "-p", "myprops.properties", "-g", "/abc/def.ghi" };
		Properties options = Loader.parseOptions(args1);
		assertEquals("/abc/def.ghi", options.getProperty("script"));		
	}
	
	@Test (expected = Exception.class)
	public void testParseExtraOption() throws Exception {
		String[] args1 = { 
			"-p", "myprops.properties", "-f", "/abc/def.ghi", "blah" };
		Properties options = Loader.parseOptions(args1);
		assertEquals("/abc/def.ghi", options.getProperty("script"));		
	}
	
	@Test
	public void testParseCommand() throws Exception {
		String[] args1 = { "-p", "myprops.properties", "-e", "load cmn_location truncate" };
		Properties opts1 = Loader.parseOptions(args1);
		assertEquals("load cmn_location truncate", opts1.getProperty("command"));
		String[] args2 = { "-p", "myprops.properties", "-e", "load", "cmn_location", "truncate" };
		Properties opts2 = Loader.parseOptions(args2);
		assertEquals("load cmn_location truncate", opts2.getProperty("command"));
	}
	
	@After
	public void tearDownAfter() throws SQLException {
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		DB.rollback();
	}

}
