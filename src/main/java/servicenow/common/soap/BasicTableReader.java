package servicenow.common.soap;

import java.io.IOException;
import java.util.NoSuchElementException;

import servicenow.common.soap.BasicTableReader;
import servicenow.common.soap.KeyList;
import servicenow.common.soap.KeyReader;
import servicenow.common.soap.Parameters;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.SoapResponseException;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableReader;

/**
 * Used to read records from ServiceNow; suitable for queries of all sizes.
 * <p/>
 * This class first uses the <b>getKeys</b> method to fetch all the 
 * qualifying sys_ids.  
 * It then fetches the records in chunks using an encoded query
 * of the form <code>sys_idIN</code><i>sys_id1,sys_id2,...</i>.
 * <p/>
 * This technique follows ServiceNow's
 * <a href="http://wiki.servicenow.com/index.php?title=Web_Services_Integrations_Best_Practices#Queries"
 * >Best Practices for Queries</a> 
 * and providing consistent performance for large tables.
 */
public class BasicTableReader extends TableReader {

	KeyList keys = null;
	int firstRow = 0;
	
	public BasicTableReader(Table table) throws IOException {
		super(table);
	}

	public BasicTableReader(Table table, QueryFilter filter) throws IOException {
		super(table);
		this.filter = filter;
	}

	/**
	 * Creates a {@link BasicTableReader} from a list of keys,
	 * avoiding the initial call to the <b>getKeys</b> method.
	 * @param table The table to be read.
	 * @param keys List of keys used to initialize this reader.
	 * @throws IOException
	 */
	public BasicTableReader(Table table, KeyList keys) throws IOException {
		super(table);
		assert keys != null;
		this.keys = keys;
	}

	public BasicTableReader setFilter(String filter) {
		super.setFilter(filter);
		return this;
	}
		
	/**
	 * Causes the reader to begin returning records
	 * from the middle a record set.
	 * @param num Zero based index of the first record
	 * to be returned by the next call to {@link #nextChunk()}.
	 */
	public void setFirstRowIndex(int num) {
		if (num < 0) 
			throw new IllegalArgumentException("firstRow=" + num);
		table.requestlog.info("setFirstRow " + num);
		this.firstRow = num; 
	}
	
	/**
	 * Returns the zero based index of the first record
	 * that will be returned by the next call to
	 * {@link #nextChunk()}.
	 */
	public int getFirstRowIndex() { 
		return this.firstRow; 
	}
	
	/**
	 * This method returns the complete list of keys
	 * for all records that will be retrieved by this reader.
	 * This method returns all the keys; not simply the keys 
	 * in the next chunk nor the list of remaining keys.
	 */
	public KeyList getKeys() 
			throws IOException, InterruptedException, SoapResponseException {
		if (this.keys != null) return this.keys;
		this.started = true;
		KeyReader keyreader = new KeyReader(this.table, this.filter, this.sort);
		this.keys = keyreader.getAllKeys();
		logger.debug("getAllKeys size=" + keys.size());
		return this.keys;
	}

	/**
	 * This method is only valid following a call to 
	 * {@link #getKeys()} and it is equivalent to
	 * <tt>getKeys().size()</tt>.
	 */
	public Integer numKeys() {
		if (keys == null) throw new IllegalStateException(
			"getKeys has not yet been called");
		return keys.size();
	}
		
	public RecordList nextChunk() 
			throws IOException, InterruptedException, 
				SoapResponseException, NoSuchElementException {
		if (finished) throw new NoSuchElementException("no more data");
		assert chunkSize > 1;
		started = true;
		RecordList chunk;
		int lastRow = firstRow + chunkSize;
		if (keys == null) getKeys();
		if (lastRow > keys.size()) lastRow = keys.size();
		if (lastRow > firstRow) {
			QueryFilter keyfilter = keys.filter(firstRow, lastRow);
			Parameters params = new Parameters(keyfilter.asParameters());
			params.add(getSortParam());
			params.add(getViewParam());
			if (Thread.interrupted()) throw new InterruptedException();
			chunk = table.getRecords(params);
			recordsRead += chunk.size();
		}
		else {
			// return an empty RecordList
			chunk = new RecordList();				
		}
		finished = (lastRow >= keys.size());
		firstRow = lastRow;
		return chunk;
	}	

}
