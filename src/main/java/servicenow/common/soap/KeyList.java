package servicenow.common.soap;

import java.util.ArrayList;
import java.util.Hashtable;

import servicenow.common.soap.Key;
import servicenow.common.soap.QueryFilter;

/**
 * Holds a list of <b>sys_id</b>s (GUIDs) 
 * as returned from a <b>getKeys</b> Web Services call. 
 * <p/>
 * @see <a href=
 * "http://wiki.servicenow.com/index.php?title=Direct_Web_Service_API_Functions#getKeys"
 * >http://wiki.servicenow.com/index.php?title=Direct_Web_Service_API_Functions</a>
 * 
 */
public class KeyList extends ArrayList<Key> {

	private static final long serialVersionUID = 1L;

	public KeyList() {
		super();
	}
	
	public KeyList(int size) {
		super(size);
	}
	
	/**
	 * Returns the complete list as a comma separated list of sys_ids.
	 */
	public String toString() {
		return getSlice(0, size());
	}
	
	/**
	 * Returns a subset of the list as comma separated string.
	 * Used to construct encoded queries.
	 * The number of entries returned is (toIndex - fromIndex).
	 * An exception may occur if toIndex < 0 or fromIndex > size() 
	 * 
	 * @param fromIndex Zero based starting index (inclusive).
	 * @param toIndex Zero based ending index (exclusive).
	 * @return A comma separated list of sys_ids suitable for use in 
	 * constructing an encoded query.
	 */
	private String getSlice(int fromIndex, int toIndex) {
		StringBuilder result = new StringBuilder();
		for (int i = fromIndex; i < toIndex; ++i) {
			if (i > fromIndex) result.append(",");
			result.append(get(i).toString());
		}
		return result.toString();
	}
	
	/**
	 * Returns a {@link QueryFilter}
	 * that selects all the records in a subset of this list.
	 * @param fromIndex Zero based starting index (inclusive).
	 * @param toIndex Zero based ending index (exclusive).
	 */
	public QueryFilter filter(int fromIndex, int toIndex) {
		String queryStr = "sys_idIN" + this.getSlice(fromIndex, toIndex);
		return new QueryFilter(queryStr);
	}
	
	public QueryFilter queryFilter() {
		return this.filter(0,  this.size());
	}
	
	/**
	 * Return the number of unique values in this list of keys.
	 */
	int uniqueCount() {
		Hashtable<Key,Boolean> hash = new Hashtable<Key,Boolean>(this.size());
		for (Key key : this) {
			hash.put(key, true);
		}
		return hash.size();
	}
}
