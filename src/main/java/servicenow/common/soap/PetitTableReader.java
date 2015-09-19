package servicenow.common.soap;

import java.io.IOException;
import java.util.NoSuchElementException;

import servicenow.common.soap.BasicTableReader;
import servicenow.common.soap.KeyList;
import servicenow.common.soap.Parameters;
import servicenow.common.soap.PetitTableReader;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.SoapResponseException;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableReader;

/**
 * A {@link TableReader} which attempts to read a set of records
 * using a single Web Service call.
 * <p/>
 * Use a {@link PetitTableReader} only if...
 * <ul>
 * <li>the number of records to be read is small, and</li>
 * <li>there is no possibility of an access control
 * which could block the ability to read some of the records.</li>
 * </ul>
 * <p/>
 * A {@link PetitTableReader} does NOT precede the first read with a getKeys call.
 * It simply starts reading the rows using first_row / last_row windowing.
 * If the number of records returned is equal to the limit,  
 * then it assumes there are more records and it keeps on reading.
 * If the number of records returned is less than the limit,
 * then it assumes it has reached the end. 
 * <p/>
 * For small result sets {@link PetitTableReader} will perform better than 
 * {@link TableReader} because it saves a Web Service call.
 * However, the performance of the {@link PetitTableReader} will degrade exponentially
 * as the number of records grows.
 * <p/>
 * <b>Warning:</b> If access controls are in place, the <b>getRecords</b> method
 * will sometimes return fewer records than the limit even though
 * there are more records to be read.  This will cause the {@link PetitTableReader}
 * to terminate prematurely. Use a {@link BasicTableReader} 
 * if there is any possibility of access controls which could cause this behavior.
 * 
 * @author Giles Lewis
 *
 */
public class PetitTableReader extends TableReader {

	int firstRow = 0;
	
	protected PetitTableReader(Table table) throws IOException {
		super(table);
	}

	protected PetitTableReader(Table table, QueryFilter filter) throws IOException {
		this(table);
		this.filter = filter;
	}

	public PetitTableReader setFilter(String filter) {
		super.setFilter(filter);
		return this;
	}

	/**
	 * Return params for __encoded_query, __first_row and __last_row
	 */
	private Parameters getWindowParams(int firstRow, int lastRow) {
		Parameters params = new Parameters();
		params.add("__first_row", Integer.toString(firstRow));
		params.add("__last_row", Integer.toString(lastRow));
		return params;
	}
	
	public RecordList nextChunk() 
			throws IOException, SoapResponseException, NoSuchElementException {
		if (finished) throw new NoSuchElementException("no more data");
		assert chunkSize > 1;
		started = true;
		RecordList chunk;
		int lastRow = firstRow + chunkSize;
		Parameters params = new Parameters();
		params.add(getFilterParam());
		params.add(getSortParam());
		params.add(getViewParam());
		params.add(getWindowParams(firstRow, lastRow));
		chunk = table.getRecords(params);
		recordsRead += chunk.size();
		finished = (chunk.size() < chunkSize);
		firstRow = lastRow;
		return chunk;
	}
	
	public KeyList getKeys() 
			throws IOException, SoapResponseException {
		throw new UnsupportedOperationException();
	}
	
}
