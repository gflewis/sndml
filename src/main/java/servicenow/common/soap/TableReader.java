package servicenow.common.soap;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.slf4j.Logger;

import servicenow.common.soap.KeyList;
import servicenow.common.soap.Parameters;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.QuerySort;
import servicenow.common.soap.Record;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.SoapResponseException;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableReader;

/**
 * Used to read records from ServiceNow in chunks.
 * </p>
 * ServiceNow limits the number of records which can be retrieved 
 * in a single <b>getRecords</b> call.
 * Therefore, when reading an unknown amount of data,
 * the records must be processed in a double loop as follows.  
 * The outer loop fetches a chunk of records from ServiceNow.  
 * The inner loop processes the records in the chunk.
 * <p/>
 * <pre>
 * {@link TableReader} reader = table.reader(filter);
 * while (reader.hasNext()) {
 *     {@link RecordList} chunk = reader.nextChunk();
 *     for ({@link Record} rec : chunk) {
 *         // insert code here to process record
 *     }
 * }
 * </pre>
 * To read all the records in a table into a List, use the
 * {@link TableReader#getAllRecords() getAllRecords()} method. 
 * 
 * <pre>
 * RecordList locations = 
 *     session.table("cmn_location").reader().getAllRecords();
 * </pre>
 * 
 * To read records subject to a filter, use the 
 * {@link #setFilter(String) setFilter()} method.
 * 
 * <pre>
 * RecordList incidents = 
 *     session.table("incident").reader().setFilter("active=true").getAllRecords();
 * </pre>
 * 
 * <p/>
 * 
 */
public abstract class TableReader implements Iterator<RecordList>{

	protected final Table table;
	protected int chunkSize;
	protected boolean started = false;
	protected boolean finished = false;
	protected int recordsRead = 0;
	protected QueryFilter filter = null;
	protected QuerySort sort = null;
	protected String viewName = null;
		
	final Logger logger;
	
	/**
	 * This method is for internal use.
	 * To obtain a {@link TableReader} use
	 * {@link Table#reader()}.
	 */
	protected TableReader(Table table) throws IOException {
		assert table != null;
		this.table = table;
		this.chunkSize = table.chunkSize;
		this.logger = table.responselog;
	}
	
	/**
	 * Sets the query filter for a record reader.
	 * This method must be called before starting record retrieval.
	 * 
	 * @param filter an encoded query
	 * @return The modified reader.
	 */
	public TableReader setFilter(QueryFilter filter) {
		if (started) throw new IllegalStateException();
		this.filter = filter;
		return this;
	}
	
	/**
	 * Sets the query filter.
	 * This method must be called before starting record retrieval.
	 * 
	 * @param filter An encoded query.
	 * For an explanation of encoded query syntax refer to
	 * <a href="http://wiki.servicenow.com/index.php?title=Reference_Qualifiers"
	 * >http://wiki.servicenow.com/index.php?title=Reference_Qualifiers</a>
	 * @return The modified reader.
	 */
	public TableReader setFilter(String filter) {
		if (started) throw new IllegalStateException();
		this.filter = (filter == null ? null : new QueryFilter(filter));
		return this;
	}
	
	/**
	 * Augments the query filter
	 * by adding an additional AND ('^') condition.
	 * @return The modified reader.
	 */
	public TableReader addFilter(QueryFilter newfilter) {
		if (started) throw new IllegalStateException();
		if (filter == null)
			filter = new QueryFilter(newfilter);
		else
			filter.addFilter(newfilter);
		return this;
	}
	
	/**
	 * Augments the query filter
	 * by adding an additional AND ('^') condition.
	 * @return The modified reader.
	 */
	public TableReader addFilter(String newfilter) {
		if (started) throw new IllegalStateException();
		return addFilter(new QueryFilter(newfilter));		
	}

	public TableReader setView(String viewName) {
		if (started) throw new IllegalStateException();
		this.viewName = viewName;
		return this;
	}
	
	/**
	 * Cancels a previous
	 * {@link #sortAscending(String)}
	 * or
	 * {@link #sortDescending(String)}.
	 */
	public TableReader setNoSort() {
		if (started) throw new IllegalStateException();
		this.sort = null;
		return this;
	}
	
	/**
	 * Requests the retrieved records to be returned 
	 * in ascending order by the specified fieldname.
	 */
	public TableReader sortAscending(String fieldname) {
		if (started) throw new	IllegalStateException(
			"cannot change sort after starting retrieval");
		this.sort = QuerySort.ascending(fieldname);
		if (table.validate)	this.sort.validate(table);
		return this;
	}

	/**
	 * Requests the retrieved records to be returned 
	 * in descending order by the specified field name.
	 */
	public TableReader sortDescending(String fieldname) {
		if (started) throw new	IllegalStateException(
			"cannot change sort after starting retrieval");
		this.sort = QuerySort.descending(fieldname);
		if (table.validate) this.sort.validate(table);
		return this;
	}

	/**
	 * Requests the retrieved records to be returned
	 * in ascending order by sys_created_on.
	 */
	public TableReader sortByCreateDate() {
		sortAscending("sys_created_on");
		return this;
	}

	/**
	 * Requests the retrieved records to be returned
	 * in ascending order by sys_updated_on.
	 */
	public TableReader sortByUpdateDate() {
		sortAscending("sys_updated_on");
		return this;
	}
	
	/**
	 * Sets the maximum number of records that will be returned
	 * in each call to {@link #nextChunk()}.
	 * @return The modified reader.
	 */
	public TableReader setChunkSize(int size) {
		table.requestlog.info("setLimit " + size);
		if (size < 1) 
			throw new IllegalArgumentException("limit=" + size);
		this.chunkSize = size;
		return this;
	}

	/**
	 * Returns true if {@link #nextChunk()} can be safely called
	 * to return the next chunk of records.
	 * This does not guarantee that there are more records
	 * as {@link #nextChunk()} may return an empty 
	 * {@link RecordList}.
	 */
	public boolean hasNext() {
		return !finished;
	}

	/**
	 * Returns true if data retrieval has started.
	 * Once data retrieval has started, any attempt 
	 * to change properties such as the filter or the sort
	 * will result in an IllegalStateException.
	 */
	public boolean started() {
		return started;
	}

	/**
	 * Returns true if all records have been retrieved.
	 * This is the opposite of {@link #hasNext()}.
	 * @return true if all records have been retrieved
	 */
	public boolean finished() {
		return finished;
	}
		
	/**
	 * An alias for {@link #nextChunk()} as required to support the Iterator interface.
	 */
	public RecordList next() 
			throws NoSuchElementException {
		try {
			return nextChunk();
		} catch (SoapResponseException e) {
			e.printStackTrace();
			throw new UndeclaredThrowableException(e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new UndeclaredThrowableException(e);
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new UndeclaredThrowableException(e);
		}
	}
	
	/**
	 * This method initiates record retrieval 
	 * (if it is not already started)
	 * and returns the next chunk of records as a {@link RecordList}.
	 * <p/>
	 * <b>Note:</b>
	 * The {@link #hasNext()} method
	 * should be called before calling this method to determine if 
	 * this method can be safely called.
	 * If {@link #hasNext()} returns false
	 * then this method can throw
	 * {@link java.lang.IllegalStateException}.
	 * <p/>
	 * This method may return an empty {@link RecordList}
	 * if no more records are available.
	 * 
	 * @return The next chunk of records as a {@link RecordList}.
	 * The returned list may be empty if no more records are available.
	 * @throws IllegalStateException
	 * Thrown if {@link #hasNext()} returns false.
	 */
	public abstract RecordList nextChunk()
			throws IOException, InterruptedException, 
				SoapResponseException, NoSuchElementException;

	/**
	 * This method loops internally on the TableReader until no more records
	 * are available.  The easiest way to read an entire table
	 * into memory is to use the following construct:
	 * <pre>
	 * RecordList recs = table.reader().getAllRecords();
	 * </pre>
	 * <b>Warning: This method will read all records into memory.
	 * Do not use this method on large tables
	 * unless you have set a filter to limit the number of records.</b><p/>
	 * 
	 * @return A RecordList of all records matching the TableReader filter.
	 * @throws IOException
	 */
	public RecordList getAllRecords() 
			throws IOException, InterruptedException, SoapResponseException {
		RecordList result = new RecordList();
		int count = 0;
		while (hasNext()) {
			RecordList chunk = nextChunk();
			count += chunk.size();
			logger.info("getAllRecords " + count + " records");
			result.addAll(chunk);
		}
		logger.info("getAllRecords " + result.size() + " records (total)");
		return result;
	}

	/**
	 * Return __encoded_query parameter or null if there is no filter.
	 */
	protected Parameters getFilterParam() {
		if (filter == null || filter.isEmpty()) return null;
		return filter.asParameters();
	}
	
	/**
	 * Return __order_by parameter or null if there is not sort.
	 */
	protected Parameters getSortParam() {
		if (sort == null || sort.isEmpty()) return null;
		return sort.asParameters();
	}
	
	/**
	 * Return __use_view parameter or null if there is no view.
	 */
	protected Parameters getViewParam() {
		if (viewName == null) return null;
		Parameters params = new Parameters("__use_view", viewName);
		return params;
	}

	@Deprecated
	public abstract KeyList getKeys() 
			throws IOException, InterruptedException, SoapResponseException;
	
	/**
	 * Unsupported operation.
	 * @throws UnsupportedOperationException
	 */
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
