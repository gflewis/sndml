package servicenow.common.soap;

import servicenow.common.soap.DateTime;
import servicenow.common.soap.Parameters;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.TableReader;

/**
 * Encapsulates an Encoded Query.  
 * <p/>
 * A {@link QueryFilter} is used to restrict the number
 * of records return by a {@link TableReader}.  
 * The following two examples are equivalent.
 * <p/>
 * <b>Example 1:</b>
 * <pre>
 * QueryFilter filter = new QueryFilter("category=network^active=true");
 * </pre>
 * <p/>
 * <b>Example 2:</b>
 * <pre>
 * QueryFilter filter = new QueryFilter()
 *    .addFilter("category", "network")
 *    .addFilter("active", "true");
 * </pre>
 * 
 * Most of the methods in the class will return the modified object
 * so that it is easy to chain calls together as in Example 2 above.
 * <p/>
 * For an explanation of encoded query syntax refer to
 * <a href="http://wiki.servicenow.com/index.php?title=Reference_Qualifiers"
 * >http://wiki.servicenow.com/index.php?title=Reference_Qualifiers</a>
 * <p/>
 * <b>Warning:</b>
 * This class does NOT check the syntax of the encoded query string.
 * If you construct an encoded query with an invalid field name or
 * an invalid syntax, the behavior of ServiceNow will be to ignore it.
 * 
 * @author Giles Lewis
 */
public class QueryFilter {

	// Here are some commonly used encoded query operators
	final public static String EQUALS       = "=";
	final public static String NOT_EQUAL    = "!=";
	final public static String LESS_THAN    = "<";
	final public static String GREATER_THAN = ">";
	final public static String STARTS_WITH  = "STARTSWITH";
	final public static String CONTAINS     = "LIKE";
	final public static String IN           = "IN";
	final public static String NOT_IN       = "NOT IN";
	
	private StringBuffer query;

	public QueryFilter() {
		this.query = new StringBuffer();
	}
	
	/**
	 * Create a filter from an Encoded Query string.
	 * 
	 * <pre>
	 * QueryFilter filter = new QueryFilter("category=network^active=true");
	 * </pre>
	 * 
	 * @param encodedQuery Encoded query string
	 */
	public QueryFilter(String encodedQuery) {
		if (encodedQuery == null)
			this.query = new StringBuffer();
		else 
			this.query = new StringBuffer(encodedQuery);
	}

	public QueryFilter(String field, String relop, String value) {
		this();
		addFilter(field, relop, value);
	}
	
	public QueryFilter(String field, String value) {
		this();
		addFilter(field, value);
	}
	
	/**
	 * Make a copy of a QueryFilter
	 */
	public QueryFilter(QueryFilter filter) {
		this.query = new StringBuffer(filter.query);
	}

	public boolean isEmpty() {
		return (this.query.length() == 0);
	}
	
	/**
	 * Augment a QueryFilter using a name, operator and value.
	 * 
	 * <pre>
	 * QueryFilter filter = new QueryFilter()
	 *    .addFilter("category", "=", "network")
	 *    .addFilter("active", "=", "true");
	 * </pre>
	 * 
	 */
	public QueryFilter addFilter(String field, String relop, String value) {
		assert field != null;
		assert relop != null;
		if (value == null)
			return addFilter(field + relop);
		else
			return addFilter(field + relop + value);
	}
	
	/**
	 * Augment a QueryFilter using a name/value pair.
	 */
	public QueryFilter addFilter(String field, String value) {
		return addFilter(field, EQUALS, value);
	}
	
	/**
	 * Adds a datetime range to a filter for sys_updated_on.
	 * 
	 * @param starting Select records updated on or after this datetime
	 * @param ending Select records updated before this datetime
	 * @return The modified original filter
	 */
	public QueryFilter addUpdatedFilter(DateTime starting, DateTime ending) {
		if (starting != null) this.addFilter("sys_updated_on>=" + starting.toString());
		if (ending   != null) this.addFilter("sys_updated_on<" + ending.toString());
		return this;
	}

	/**
	 * Adds a datetime range to a filter for sys_created_on.
	 * 
	 * @param starting Select records created on or after this datetime
	 * @param ending Select records created before this datetime
	 * @return The modified original filter
	 */
	public QueryFilter addCreatedFilter(DateTime starting, DateTime ending) {
		if (starting != null) this.addFilter("sys_created_on>=" + starting.toString());
		if (ending   != null) this.addFilter("sys_created_on<" + ending.toString());
		return this;
	}

	/**
	 * Add an encoded query to a QueryFilter
	 * 
	 * <pre>
	 * filter.addFilter("category=network");
	 * </pre>
	 * 
	 * @param newQuery An encoded query string
	 * @return The modified original filter
	 */
	public QueryFilter addFilter(String newQuery) {
		if (newQuery != null) {
			if (this.query.length() > 0) this.query.append("^");
			this.query.append(newQuery);
		}
		return this;
	}
	
	public QueryFilter addFilter(QueryFilter newQuery) {		
		if (newQuery != null) addFilter(newQuery.toString());
		return this;
	}

	public String toString() {
		return this.query.toString();
	}
	
	/**
	 * Return this filter converted to an __encoded_query parameter
	 * or null if this filter is empty
	 */
	Parameters asParameters() {
		if (isEmpty()) return null;
		return new Parameters("__encoded_query", query.toString());
	}
}
