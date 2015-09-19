package servicenow.common.soap;

import java.net.HttpURLConnection;
import java.util.Enumeration;
import java.util.Hashtable;

import org.slf4j.Logger;

import servicenow.common.soap.CookieJar;
import servicenow.common.soap.Session;

/**
 * Used to hold the JSESSIONID.
 * <p/>
 * This class is used to hold HTTP cookies.  It knows how to read cookies
 * from an HttpURLConnection and write cookies from an HttpConnection. 
 * In the context of this application the CookieJar contains only a single cookie:
 * JSESSIONID. 
 * 
 * @author Giles Lewis
 */
public class CookieJar {

	final static Logger log = Session.getLogger(CookieJar.class);

	Hashtable<String,String> jar = new Hashtable<String,String>();

	/**
	 * Reads all cookies from an HttpURLConnection and save them in the jar
	 * 
	 * @param connection HTTP Connection from which the cookie will be retrieved
	 * @param clear If true then empty the jar before loading
	 */
	public synchronized void addResponseCookies(HttpURLConnection connection, boolean clear) {
		if (clear) jar.clear();
		boolean done = false;
		for (int i = 1; !done; ++i) {
			String headerName = connection.getHeaderFieldKey(i);
			if (headerName == null) 
				done = true;
			else {
				if (headerName.equals("Set-Cookie")) {
			 		String headerValue = connection.getHeaderField(i);
			        headerValue = headerValue.substring(0, headerValue.indexOf(";"));
			        int equalsign = headerValue.indexOf("=");
			        String cookieName = headerValue.substring(0, equalsign);
				    String cookieValue = headerValue.substring(equalsign + 1, headerValue.length());
				    addResponseCookie(cookieName, cookieValue);
				}
			}
		}
	}
	
	/**
	 * Add a cookie to the jar or update the value of a cookie.
	 * 
	 * @param name Name of cookie
	 * @param value String value of cookie
	 */
	void addResponseCookie(String name, String value) {
		jar.put(name, value);
		log.debug("addResponseCookie " + name + "=" + value);
	}

	/**
	 * Get the value of a cookie by name.
	 */
	String getCookie(String name) {
		String result = jar.get(name);
		log.debug("getCookie " + name + "=" + result);
		return result;
	}

	/**
	 * Return true if this jar contains no cookies.
	 */
	boolean isEmpty() {
		return jar.isEmpty();
	}
	
	/**
	 * Return all the cookies in the jar as a string 
	 * with semicolons between the cookies.
	 */
	String getAllCookies() {
		StringBuilder cookies = new StringBuilder();
		Enumeration<String> e = jar.keys();
		while (e.hasMoreElements()) {
			String key = e.nextElement();
			String value = key + "=" + jar.get(key);
			if (cookies.length() > 0) cookies.append("; ");
			cookies.append(value);
		}
		return cookies.toString();
	}
	
	/**
	 * Write all the cookies in the jar to the HttpURLConnection.
	 * 
	 * @param connection HTTP Connection to which cookies will be posted using setRequestProperty
	 */
	public synchronized void setRequestCookies(HttpURLConnection connection) {
		if (isEmpty()) {
			log.debug("setRequestCookies (cookiejar is empty)");
		}
		else {
			String cookies = getAllCookies();
			connection.setRequestProperty("Cookie", cookies);		
			log.debug("setRequestCookies " + cookies);
		}
			
	}
}
