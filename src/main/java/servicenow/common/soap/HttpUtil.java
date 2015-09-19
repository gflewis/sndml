package servicenow.common.soap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.util.zip.GZIPInputStream;

/**
 * Static functions for reading an HttpURLConnection
 */
class HttpUtil {

	/**
	 * Read the entire contents of a java.io.Reader into a String
	 */
	static String readFully(Reader reader) throws IOException {
	    final int BUFFER_SIZE = 4096;	    
		StringBuilder buffer = new StringBuilder();
		char[] cbuf = new char[BUFFER_SIZE]; int len;
		while ((len = reader.read(cbuf, 0, BUFFER_SIZE)) >= 0) {
			buffer.append(cbuf, 0, len);
		}
		return buffer.toString();
	}
	
	/**
	 * Read an InputStream, inflating if necessary, and returning a String
	 * 
	 * @param input InputStream from the HTTP Connection
	 * @param gzip true if encoding=gzip, otherwise false
	 * @return Inflated contents of the InputStream
	 */
	static String readFully(InputStream input, boolean gzip) throws IOException {
		InputStream myinput = gzip ? new GZIPInputStream(input) : input;
		return readFully(new BufferedReader(new InputStreamReader(myinput)));
	}
	
	/**
	 * Read the connection input into a String, inflating if necessary
	 */
	static String readFully(HttpURLConnection connection) throws IOException {
		String responseEncoding = connection.getContentEncoding();
		boolean responseCompressed = "gzip".equals(responseEncoding);
		return readFully(connection.getInputStream(), responseCompressed);	
	}
	
	/**
	 * Return all HTTP headers in a format suitable for debugging.
	 */
	static String getAllHeaders(HttpURLConnection connection) {
		StringBuilder buf = new StringBuilder();
		int i = 0; String key; String value;
		do {
			key = connection.getHeaderFieldKey(i);
			value = connection.getHeaderField(i++);
			if (key != null || value != null) {
				if (key != null) {
					buf.append(key); 
					buf.append("="); 
				}
				buf.append(value); 
				buf.append("\n");
			}
		}
		while (key != null || value != null);
		return buf.toString();
	}
	
}
