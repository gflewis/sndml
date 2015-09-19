package servicenow.common.soap;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;

import servicenow.common.soap.HttpUtil;
import servicenow.common.soap.InsufficientRightsException;
import servicenow.common.soap.Session;
import servicenow.common.soap.SessionMetrics;
import servicenow.common.soap.SoapResponseException;
import servicenow.common.soap.Table;
import servicenow.common.soap.XMLFormatter;

/**
 * This is a "private" class used by {@link Table} to make Web Service calls.  
 * Its purpose is to hide the nuances of the SOAP protocol from the Table class 
 * which deals only with simpler JDOM objects.
 * 
 * @author Giles Lewis
 */
class TableProxy {

	final Table table;
	final Session session;
	java.net.URL url;
	final boolean compression;
	Integer timeoutMillisec;
	SessionMetrics metrics;
	
	private final Logger requestlog;
	private final Logger responselog;
	
	private static ThreadLocal<SAXBuilder> localParser =
		new ThreadLocal<SAXBuilder>() {
			protected SAXBuilder initialValue() {
				return new SAXBuilder();
			}
	};
		
	final static Namespace nsSoapEnv = 
		Namespace.getNamespace("env", "http://schemas.xmlsoap.org/soap/envelope/"); 	
    final static Namespace nsSoapEnc = 
    	Namespace.getNamespace("enc", "http://schemas.xmlsoap.org/soap/encoding/");	

    public TableProxy(Table table) throws IOException {
		this.table = table;
		this.session = table.session;
		this.compression = table.getConfiguration().getBoolean("compression", true);
		this.timeoutMillisec = table.getConfiguration().getInteger("timeout", null);
		this.requestlog = table.requestlog;
		this.responselog = table.responselog;
		this.metrics = session.metrics;
		setURL(false);
    }

    synchronized void setURL(boolean dv) throws MalformedURLException {
    	ArrayList<String> params = new ArrayList<String>(2);
    	if (dv) params.add("displayvalue=all");
    	params.add("SOAP");
    	this.url = session.getURL(table.getName(), params);
    }
       
	private Document createSoapDocument(Element content) {
	    Element requestHeader = new Element("Header", nsSoapEnv);
	    Element requestBody = new Element("Body", nsSoapEnv);
	    requestBody.addContent(content);    
	    Element envelope = new Element("Envelope", nsSoapEnv);
		envelope.addNamespaceDeclaration(nsSoapEnv);
		envelope.addNamespaceDeclaration(nsSoapEnc);
		envelope.addNamespaceDeclaration(content.getNamespace());
		envelope.addContent(requestHeader);
		envelope.addContent(requestBody);
		return new Document(envelope);
	}
	
	public Element callSoap(
		Element content, 
		String responseElementName)
			throws IOException, SoapResponseException {
		
		String methodname = content.getName();
		Document requestDoc = createSoapDocument(content);

		HttpURLConnection connection = session.openConnection(url);
		if (compression) connection.setRequestProperty("Accept-Encoding", "gzip");
		if (timeoutMillisec != null) {
			connection.setConnectTimeout(timeoutMillisec);
			connection.setReadTimeout(timeoutMillisec);
			requestlog.debug("timeout=" + timeoutMillisec);
		}
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		
		String requestText = XMLFormatter.format(requestDoc, false);
		requestlog.debug(requestText);
		
		connection.connect();
		connection.getOutputStream().write(requestText.getBytes());
	    
		int responseCode = connection.getResponseCode();
		int responseLength = connection.getContentLength();
		String responseEncoding = connection.getContentEncoding();
		boolean responseCompressed = "gzip".equals(responseEncoding);

		if (responselog.isDebugEnabled()) {
			responselog.debug(HttpUtil.getAllHeaders(connection));
		}
				
		SAXBuilder parser = localParser.get();
		
		if (responseCode != HttpURLConnection.HTTP_OK) {
			String responseMessage = connection.getResponseMessage();
			String message = "responseCode=" + responseCode + " " + responseMessage;
			String faultString = null;
			try {
				InputStream errStream = connection.getErrorStream();
				if (errStream != null) {
					String errBuffer = HttpUtil.readFully(errStream, responseCompressed);
					
					Document faultDoc = parser.build(new StringReader(errBuffer));
					faultString = faultDoc.getRootElement().
						getChild("Body", nsSoapEnv).
						getChild("Fault", nsSoapEnv).
						getChildText("faultstring");
					responselog.warn(faultString);
				}
			}
			catch (IOException e) {}
			catch (JDOMException e) {}
			if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP) // 302 error
				message = message +  
					"\nRequest  Location=" + url.toString() + 
					"\nResponse Location=" + connection.getHeaderField("Location") + 
					"\nRedirect is not supported by this API";
			responselog.error(faultString == null ? message : message + "\n" + faultString);
			if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) // 400 error				
				throw new SoapResponseException(table, message, requestText);
			else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || // 401 error
					responseCode == HttpURLConnection.HTTP_FORBIDDEN) // 403 error					
				throw new InsufficientRightsException(table, methodname, message);
			// this is a bit of a kludge
			// however, permissions errors are sufficiently common that it is worth 
			// making some attempt to detect them and throw an InsufficientRightsException
			else if (faultString != null && faultString.toLowerCase().indexOf("insufficient rights") > -1)
				throw new InsufficientRightsException(table, methodname, faultString);
			else
				throw new SoapResponseException(table, message, faultString);
		}

		String responseBuffer = HttpUtil.readFully(connection.getInputStream(), responseCompressed);
		responselog.debug(" responseLength=" + responseLength + 
			" bufferLength=" + responseBuffer.length());
		
		Document responseDoc = null;		
		try {
			responseDoc = parser.build(new StringReader(responseBuffer));
		} catch (JDOMException e) {
			if (responseBuffer != null) responselog.error(responseBuffer);
			throw new SoapResponseException(table, e, responseBuffer);
		}
		if (responselog.isDebugEnabled()) {
			responselog.debug(XMLFormatter.format(responseDoc));
		}
	
		Element responseBody = responseDoc.getRootElement().getChild("Body", nsSoapEnv);
		Element result = responseBody.getChild(responseElementName);
		if (result == null) {
			responselog.error("Missing element: " + responseElementName + "\n" + responseBuffer);
			throw new SoapResponseException(table, methodname, responseBuffer);
		}
		
		session.updateSessionInfo(connection);
		metrics.increment(table.getName(), methodname);
	    return result;
	}
		
}
