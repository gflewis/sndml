package servicenow.common.soap;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import servicenow.common.soap.DateTime;
import servicenow.common.soap.InvalidDateTimeException;

/**
 * An immutable class that holds a DateTime field in the string format required 
 * by the XML. This class can convert the value to or from a Java Date.
 * All DateTime fields are represented in GMT.
 */
public class DateTime implements Comparable<DateTime> {

	public static final int DATE_ONLY = 10; // length of yyyy-MM-dd
	public static final int DATE_TIME = 19; // length of yyyy-MM-dd HH:mm:ss
	
	static ThreadLocal<DateFormat> dateOnlyFormat = 
			new ThreadLocal<DateFormat>() {
				protected DateFormat initialValue() {
					DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
					df.setTimeZone(TimeZone.getTimeZone("GMT"));
					return df;
				}
			};

	static ThreadLocal<DateFormat> dateTimeFormat = 
		new ThreadLocal<DateFormat>() {
			protected DateFormat initialValue() {
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				df.setTimeZone(TimeZone.getTimeZone("GMT"));
				return df;
			}
		};
		
	private final String str;
	private final Date dt;
	
	/**
	 * Construct a DateTime object. 
	 * This constructor will work if the value is 
	 * "yyyy-MM-dd" or "yyyy-MM-dd HH:mm:ss".
	 * For any other value it will throw an InvalidDateTimeException. 
	 */
	public DateTime(String value) throws InvalidDateTimeException {
		this(value, value.length());
	}

	/**
	 * Construct a DateTime object.  
	 * @param value String value to be converted
	 * @param fmtlen Specify 10 if value is "yyyy-MM-DD", 19 if value is "yyyy-MM-dd HH:mm:ss".
	 * @throws InvalidDateTimeException
	 */
	public DateTime(String value, int fmtlen) throws InvalidDateTimeException {
		DateFormat df;
		switch (fmtlen) {
		case DATE_ONLY : // 10
			df = dateOnlyFormat.get();
			break;
		case DATE_TIME : // 19
			df = dateTimeFormat.get();
			break;
		default :
			throw new InvalidDateTimeException(value);
		}
		this.str = value;
		try {
			this.dt = df.parse(this.str);
		}
		catch (ParseException e) {
			throw new InvalidDateTimeException(value);
		}
	}
	
	public DateTime(Date value) {
		DateFormat df = dateTimeFormat.get();
		this.dt = value;
		this.str = df.format(value);
	}
	
	public DateFormat getDateTimeFormat() {
		return dateTimeFormat.get();
	}
	
	public String toString() {
		return str;
	}
	
	public Date toDate() {
		return dt;
	}

	public long getMillisec() {
		return dt.getTime();
	}
	
	public long getSeconds() {
		return dt.getTime() / 1000;
	}
	
	public boolean equals(Object another) {
		return this.toString().equals(another.toString());
	}
	
	public int compareTo(DateTime another) {
		return (int) (getSeconds() - another.getSeconds());
	}

	/**
	 * Returns a new DateTime which is the original object incremented
	 * by the specified amount of time (or decremented if the argument
	 * is negative).<p/>
	 * Warning: This method does <b>NOT</b> modify the original object.
	 * DateTime objects are immutable.
	 */
	public DateTime addSeconds(int seconds) {
		long millis = this.toDate().getTime();
		millis += (1000 * seconds);
		return new DateTime(new Date(millis));		
	}
	
	/**
	 * Returns a new DateTime which is the original object decremented
	 * by the specified amount of time (or incremented if the argument
	 * is negative).<p/>
	 * Warning: This method does <b>NOT</b> modify the original object.
	 * DateTime objects are immutable.
	 */
	public DateTime subtractSeconds(int seconds) {
		return this.addSeconds(-seconds);
	}
	
	/**
	 * Returns the current time.
	 * @return Current datetime.
	 */
	public static DateTime now() {
		return new DateTime(new Date());
	}

	/**
	 * Returns the current local time adjusted by a slight lag.<p/>
	 * This function is used to compensate for bugs that can occur if
	 * the clock on the local machine is running faster than the clock
	 * on the ServiceNow instance.
	 */
	@Deprecated
	public static DateTime now(int lagSeconds) {
		return now().subtractSeconds(lagSeconds);
	}
	
	/**
	 * Returns the current GMT date.
	 * @return Midnight of the current GMT date.
	 */
	public static DateTime today() {
		DateTime result;
		DateTime now = new DateTime(new Date());
		String trunc = now.toString().substring(0, 11)  + "00:00:00";
		result = new DateTime(trunc);
		return result;
	}
	
}
