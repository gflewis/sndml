package servicenow.common.soap;

import static org.junit.Assert.*;

import java.io.*;
import java.net.*;

import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import servicenow.common.soap.FieldValues;
import servicenow.common.soap.Key;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;

public class AttachmentTest {
	
	Logger logger = AllTests.junitLogger(AttachmentTest.class);
	Session session;
	Table incident;
	Key taskid;
	
	@Before
	public void setUp() throws Exception {
		session = AllTests.getSession();
		incident = session.table("incident");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testBase64() throws Exception {
		byte [] linefeed = { '\n' };
		Base64 base64 = new Base64(76, linefeed);
		File jpg = new File(AllTests.getProperty("sample_jpg_file"));
		logger.info("file name=" + jpg.getName());
		int size = (int) jpg.length();
		logger.info("file size=" + size);
		byte [] fileData = new byte[size];
		DataInputStream dis = new DataInputStream((new FileInputStream(jpg)));
		dis.readFully(fileData);
		dis.close();
		String encoded = base64.encodeAsString(fileData);
		logger.info("base64 size=" + encoded.length());
		assertTrue(encoded.length() > jpg.length());
		logger.debug(encoded);
		FileNameMap fileNameMap = URLConnection.getFileNameMap();
		String type = fileNameMap.getContentTypeFor(jpg.getName());
		logger.debug("file type=" + type);		
	}
	
	@Test
	public void testAttachFile() throws Exception {
		File file = new File(AllTests.getProperty("sample_jpg_file"));
	    FieldValues values = new FieldValues();
	    values.put("short_description", "File attachment test");
	    taskid = incident.insert(values).getSysId();
	    assertNotNull(taskid);
		incident.attachFile(taskid, file);
	}	

}
