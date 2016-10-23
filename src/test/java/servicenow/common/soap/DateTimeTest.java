package servicenow.common.soap;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;
import org.slf4j.Logger;

import servicenow.common.soap.DateTime;
import servicenow.common.soap.InvalidDateTimeException;


public class DateTimeTest {

	Logger logger = AllTests.junitLogger(DateTimeTest.class);
	
	@Test
	public void testEqualsDateTime() {
		DateTime d1 = DateTime.now();
		DateTime d2 = DateTime.now();
		assertEquals(0, d1.compareTo(d2));
		assertTrue(d1.equals(d2));
	}

	@Test (expected = InvalidDateTimeException.class)
	public void testBadDate() {
		DateTime d1 = new DateTime("2014-01-15 abcd");
		assertNull(d1);
	}

	@Test (expected = InvalidDateTimeException.class)
	public void testEmptyDate() {
		DateTime d1 = new DateTime("");
		assertNull(d1);
	}
	

	@Test
	public void testCompareTo() throws InvalidDateTimeException {
		DateTime d1 = new DateTime("2014-01-15 12:00:00");
		DateTime d2 = new DateTime("2014-01-15 12:00:17");
		assertTrue(d2.compareTo(d1) > 0);
		assertTrue(d1.compareTo(d2) < 0);
		assertTrue(d2.compareTo(d2) == 0);
		assertEquals(17, d2.compareTo(d1));
	}

	@Test
	public void testAddSeconds() throws InvalidDateTimeException {
		DateTime d1 = new DateTime("2014-01-15 12:00:00");
		DateTime d2 = new DateTime("2014-01-15 12:00:17");
		assertTrue(d2.equals(d1.addSeconds(17)));
		assertEquals(17, d2.compareTo(d1));
		assertEquals(-17, d1.compareTo(d2));
	}
	
	@Test 
	public void testCompareEqual() {
		DateTime d1 = new DateTime("2014-05-26 15:34:53");
		DateTime d2 = new DateTime("2014-05-26 15:34:53");
		assertEquals(0, d1.compareTo(d2));
		assertEquals(0, d2.compareTo(d1));
	}
	@Test
	public void testTimeZone() throws InvalidDateTimeException {
		@SuppressWarnings("deprecation")
		Date d1 = new Date("Sat May 10 23:58:12 GMT 2014");
		DateTime d2 = new DateTime("2014-05-10 23:58:12");
		logger.info("d1=" + d1);
		logger.info("d2=" + d2.toDate().toString());
		assertEquals(d1.toString(), d2.toDate().toString());
	}
}
