package servicenow.common.datamart;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import servicenow.common.datamart.CommandBuffer;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.SuiteParseException;
import servicenow.common.soap.DateTime;
import servicenow.common.soap.InvalidDateTimeException;

/**
 * This class implements a simple parser that reads tokens from a text buffer.
 * 
 * Words (tokens) are composed of contiguous sequences of
 * letters, digits, underscores, hyphens and colons. 
 * Curly brackets can be used to enclose tokens that contain special characters.
 * Methods will throw {@link SuiteParseException} if errors are encountered.
 * 
 * @author Giles Lewis
 * 
 */
public class CommandBuffer {

	private String buffer;
	private int index;
	private int linenum;
	private int charnum;
	private int tokenlinenum;
	private int tokencharnum;
	private String nextToken;
	private String firstToken;

	static Logger logger = LoggerFactory.getLogger(CommandBuffer.class);

	public CommandBuffer(String text) {
		init(text);
	}

	/**
	 * Examples of input consumed by this method:
	 *   2014-01-15
	 *   2014-01-15 00:00:00
	 *   2014-01-15 10:45:34
	 *   now
	 *   today
	 *   
	 * The token "now" corresponds to the current date and time.
	 * The token "today" corresponds to midnight GMT.
	 *    
	 */
	public DateTime getDate() throws SuiteParseException {
		final String MIDNIGHT = "00:00:00";
		if (match("now"))	return DateTime.now();
		if (match("today"))	return DateTime.today();
		String date = peek();
		// make sure it is a valid
		try {
			new DateTime(date + " " + MIDNIGHT);
		} catch (InvalidDateTimeException e) {
			error("expected yyyy-mm-dd, encountered " + encountered());
		}
		consume(date);
		// if the date is followed by what appears to be a time then use it
		// otherwise set the time to 00:00:00
		String time = peek();
		if (time != null && time.length() > 4 && Character.isDigit(time.charAt(0)))
			consume(time);
		else
			time = MIDNIGHT;
		String datetimestring = date + " " + time;
		DateTime result = null;
		try {
			result = new DateTime(datetimestring);
		} catch (InvalidDateTimeException e) {
			error("expected yyyy-mm-dd hh:mm:ss, encountered " + encountered());
		}
		return result;
	}

	/**
	 * Returns date plus or minus an interval.
	 */
	public DateTime getDatePlus() throws SuiteParseException {
		DateTime date = getDate();
		int sign = 0;		
		if (match("minus")) sign = -1;
		else if (match("plus")) sign = +1;
		int seconds = 0;
		if (sign != 0) {
			seconds = getInterval();
			date = date.addSeconds(sign * seconds);
		}
		return date;
	}
	
	/**
	 * Returns an interval in seconds.
	 */
	public int getInterval() throws SuiteParseException {
		int result = Integer.parseInt(getToken());
		if (match("seconds")) { /* do nothing */
		} else if (match("minutes")) {
			result *= 60;
		} else if (match("hours")) {
			result *= (60 * 60);
		} else if (match("days")) {
			result *= (24 * 60 * 60);
		} else
			error("expected seconds|minutes|hours, encountered "
					+ encountered());
		return result;
	}

	/**
	 * If the next token matches a specified value, then the token is consumed
	 * and true is returned. 
	 * Otherwise the token is not consumed and false is returned.
	 * @param value the value to be matched
	 * @return true if the token is consumed, false otherwise
	 */
	public boolean match(String value) {
		if (value == null) throw new NullPointerException();
		if (!hasMore())
			return false;
		if (!peek().equals(value))
			return false;
		advanceToken();
		return true;
	}

	/**
	 * Returns true if the next token will successfully match any of
	 * a list of values. The token is not consumed.
	 */
	public boolean testAny(String[] values) {
		if (!hasMore())
			return false;
		String p = peek();
		for (String v : values) {
			if (p.equals(v))
				return true;
		}
		return false;
	}

	public String getOneOf(String[] okayValues) 
			throws SuiteParseException {
		String result = null;
		if (testAny(okayValues)) 
			result = getToken();
		else
			error("expected one of [\"" +
				StringUtils.join(okayValues, "\",\"") +
				"\"] encountered: " + encountered());
		return result;
	}
	
	private void init(String input) {
		buffer = input;
		index = 0;
		linenum = 0;
		charnum = 0;
		advanceToken();
		firstToken = peek();
		if (firstToken == null || firstToken.length() == 0)
			throw new IllegalArgumentException("input is empty");
	}

	public String getBuffer() {	return buffer; }
	public int getLineNum() { return tokenlinenum; }
	public int getCharNum() { return tokencharnum; }
	public String firstToken() { return firstToken; }

	/**
	 * Consume a specified token. 
	 * If the next token does not match the expected value then an exception is thrown.
	 * @param expected The expected value of the next token
	 * @throws SuiteParseException The next token did not match the expected value.
	 */
	public void consume(String expected) throws SuiteParseException {
		if (!match(expected))
			error("expected \"" + expected + "\", encountered " + encountered());
	}

	public boolean hasMore() {
		return nextToken != null;
	}

	public void verifyAtEnd() throws SuiteParseException {
		if (hasMore())
			error("Unexpected token encountered: " + encountered());
	}

	/**
	 * Returns the value of the next token wrapped in quotes
	 */
	public String encountered() {
		return hasMore() ? "\"" + peek() + "\"" : "<EndOfInput>";
	}

	/**
	 * Returns the value of the next token, but does not consume the token.
	 * @return The value of the next token.
	 */
	public String peek() {
		return nextToken;
	}

	/**
	 * Consumes the next token and returns its value.
	 * @return The value of the next token.
	 * @throws SuiteParseException 
	 */
	public String getToken() throws SuiteParseException {
		String result = nextToken;
		if (result == null)	error("Unexpected end of input");
		advanceToken();
		return result;
	}

	/**
	 * Consumes the next token, but does not return its value.
	 */
	private void advanceToken() {
		skipWhitespace();
		tokenlinenum = linenum;
		tokencharnum = charnum;
		int beginIndex = index;
		if (beginIndex >= buffer.length()) {
			nextToken = null;
		} else if (quoteBegin()) {
			while (!quoteEnd())
				advanceChar();
			advanceChar();
			String token = buffer.substring(beginIndex + 1, index - 1);
			nextToken = token.trim();
		} else if (wordChar()) {
			while (!atEnd() && !whitespace())
				advanceChar();
			nextToken = buffer.substring(beginIndex, index);
		} else {
			advanceChar();
			nextToken = buffer.substring(beginIndex, index);
		}
	}

	private void skipWhitespace() {
		while (!atEnd() && whitespace())
			advanceChar();
	}

	private boolean wordChar() {
		char c = buffer.charAt(index);
		if (Character.isLetterOrDigit(c)) return true;
		if (c == '_') return true;
		if (c == ':') return true;
		if (c == '-') return true;
		return false;
	}

	private boolean quoteBegin() {
		return buffer.charAt(index) == '{';
	}

	private boolean quoteEnd() {
		return buffer.charAt(index) == '}';
	}

	private boolean whitespace() {
		return Character.isWhitespace(buffer.charAt(index));
	}

	private void advanceChar() {
		if (atEnd())
			throw new IllegalStateException();
		charnum++;
		if (buffer.charAt(index) == '\n') {
			linenum++;
			charnum = 0;
		}
		index++;
	}

	private boolean atEnd() {
		return (index >= buffer.length());
	}

	private void error(String errmsg) throws SuiteParseException {
		String msg2 = errmsg + 
			"\nat line " + (tokenlinenum + 1) + " char " + (tokencharnum + 1) +
			"\nof \"" + buffer + "\"";
		logger.error(msg2);		
		throw new SuiteParseException(errmsg, buffer, tokenlinenum, tokencharnum);
	}
	
	public String toString() {
		return buffer;
	}
}
