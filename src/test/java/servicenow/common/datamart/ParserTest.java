package servicenow.common.datamart;

import static org.junit.Assert.*;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.junit.After;
import org.junit.Test;

import servicenow.common.datamart.CommandBuffer;
import servicenow.common.datamart.SuiteParseException;
import servicenow.common.soap.DateTime;

public class ParserTest {

	static Logger logger = AllTests.getLogger(ParserTest.class);
	
	String text1 = 
		"yellow blue  2014-01-15\n" + 
		"orange green 2014-01-15 11:11:11\n\n" + 
		"good-bye\n";
	
	@Test
	public void testTokenCount() throws Exception {
		CommandBuffer p = new CommandBuffer(text1);
		int count = 0;
		while (p.hasMore()) {
			logger.debug(
				p.getLineNum() + ":" + p.getCharNum() + 
				": " + p.peek());
			p.getToken();
			count++;
		}
		assertEquals(8, count);
	}

	@Test
	public void TestGoodDateTime() throws Exception {
		CommandBuffer p = new CommandBuffer(text1);
		for (int i = 0; i < 2; ++i) p.getToken();
		DateTime date1 = p.getDate();
		assertEquals(new DateTime("2014-01-15 00:00:00"), date1);
		for (int i = 0; i < 2; ++i) p.getToken();
		DateTime date2 = p.getDate();
		assertEquals(new DateTime("2014-01-15 11:11:11"), date2);
	}
	
	@Test (expected = SuiteParseException.class)
	public void TestBadDateTime() throws Exception {
		CommandBuffer p = new CommandBuffer(text1);
		for (int i = 0; i < 4; ++i) p.getToken();
		assertEquals("green", p.peek());
		p.getDate();
		fail("TestBadDate");
	}

	@Test
	public void TestGoodConsume() throws Exception {
		CommandBuffer p = new CommandBuffer(text1);
		for (int i = 0; i < 4; ++i) p.getToken();
		p.consume("green");
	}
	
	@Test (expected = SuiteParseException.class)
	public void TestBadConsume() throws Exception {
		CommandBuffer p = new CommandBuffer(text1);
		for (int i = 0; i < 4; ++i) p.getToken();
		p.consume("black");
		fail("TestBadConsume");
	}
	
	@Test
	public void TestLineNumber() throws Exception {
		CommandBuffer p = new CommandBuffer(text1);
		for (int i = 0; i < 4; ++i) p.getToken();
		String tok = p.peek();
		assertEquals("green", tok);
		assertEquals(1, p.getLineNum());
		assertEquals(7, p.getCharNum());
	}

	@After
	public void tearDownAfter() throws SQLException {
		DB.rollback();
	}
	
}
