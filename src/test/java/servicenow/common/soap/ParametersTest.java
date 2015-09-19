package servicenow.common.soap;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import servicenow.common.soap.Parameters;


public class ParametersTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCreate() {
		Parameters p1 = new Parameters();
		p1.add("animal", "giraffe");
		assertEquals("giraffe", p1.get("animal"));
		Parameters p2 = new Parameters(p1);
		p1.add("animal", "lion");
		assertEquals("lion", p1.get("animal"));
		assertEquals("giraffe", p2.get("animal"));
	}

}
