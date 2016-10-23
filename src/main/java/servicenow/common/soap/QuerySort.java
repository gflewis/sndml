package servicenow.common.soap;

import servicenow.common.soap.InvalidFieldNameException;
import servicenow.common.soap.Parameters;
import servicenow.common.soap.QuerySort;
import servicenow.common.soap.Table;

/**
 * Encapsulates the order_by parameter of a query.
 *
 */
public class QuerySort {

	protected enum SortType {NONE, ASC, DESC};
	SortType sortType = SortType.NONE;
	String sortFieldName;

	public QuerySort() {
		this.sortType = SortType.NONE;
	}

	public QuerySort(String fieldname) {
		this.sortType = SortType.ASC;
		this.sortFieldName = fieldname;
	}
	
	public QuerySort(String fieldname, SortType type) {
		this.sortType = type;
		this.sortFieldName = fieldname;
	}

	boolean isAscending() { return sortType == SortType.ASC; }
	boolean isDescending() { return sortType == SortType.DESC; }
	boolean isEmpty() { return sortType == SortType.NONE; }
	
	/**
	 * Return __order_by parameter or null if there is not order by.
	 */
	protected Parameters asParameters() {
		if (this.sortType == SortType.NONE) return null;
		Parameters params = new Parameters();
		if (this.sortType == SortType.ASC)
			params.add("__order_by", sortFieldName);
		if (this.sortType == SortType.DESC)
			params.add("__order_by_desc", sortFieldName);
		return params;
	}

	
	/**
	 * Verify that these sort parameters are valid for the specified table.
	 * @param table
	 * @throws InvalidFieldNameException
	 */
	public void validate(Table table) throws InvalidFieldNameException {
		if (sortFieldName != null)
			if (sortFieldName.indexOf(".") < 0 && sortFieldName.indexOf(",") < 0)
				if (!table.getWSDL().canReadField(sortFieldName))
					throw new InvalidFieldNameException(sortFieldName);	
	}

	public static QuerySort ascending(String fieldname) {
		return new QuerySort(fieldname, SortType.ASC);
	}
	
	public static QuerySort descending(String fieldname) {
		return new QuerySort(fieldname, SortType.DESC);
	}
	
	/**
	 * Requests the retrieved records to be returned
	 * in ascending order by sys_created_on.
	 */
	public static QuerySort byCreateDate() {
		return ascending("sys_created_on");
	}

	/**
	 * Requests the retrieved records to be returned
	 * in ascending order by sys_updated_on.
	 */
	public static QuerySort byUpdateDate() {
		return ascending("sys_updated_on");
	}	
	
}
