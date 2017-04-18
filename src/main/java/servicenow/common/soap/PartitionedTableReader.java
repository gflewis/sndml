package servicenow.common.soap;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;

import servicenow.common.soap.BasicTableReader;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.SoapResponseException;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableReader;

/**
 * A {@link TableReader} which reads a large table in chunks according to 
 * a partitioning field.  The reader first uses a COUNT aggregate to determine
 * the distinct values of the partitioning field.  It then creates a 
 * {@link BasicTableReader} for each distinct field value.
 *
 */
public class PartitionedTableReader extends TableReader {

	final private String partFieldName;
	LinkedHashMap<String,Integer> parts = null;
	Iterator<String> partsIter = null;
	TableReader partReader = null;
	String partFieldValue = null;
	int partExpectedCount;
	int partActualCount;
		
	public PartitionedTableReader(Table table, String partFieldName) throws IOException {
		super(table);
		assert table != null;
		assert partFieldName != null;
		assert partFieldName.length() > 0;
		this.partFieldName = partFieldName;
	}
	
	LinkedHashMap<String,Integer> getParts() throws SoapResponseException, IOException {
		if (parts == null) {
			parts = table.getCount(partFieldName, filter);
			assert parts != null;
			partsIter = parts.keySet().iterator();
			started = true;
		}
		return parts;
	}

	@Override
	public RecordList nextChunk() throws IOException, InterruptedException,
			SoapResponseException, NoSuchElementException {
		if (finished) throw new NoSuchElementException("no more data");
		RecordList chunk;
		if (parts == null) getParts();
		started = true;
		if (partReader == null || partReader.finished()) {
			if (partReader != null) {
				// check counts for finished part
				if (partActualCount != partExpectedCount)
					throw new AssertionError(
						"partition " + partFieldName + "=" + partFieldValue +
						" expected rows=" + partExpectedCount +
						" actual rows=" + partActualCount);
			}
			if (partsIter.hasNext()) {
				partFieldValue = partsIter.next();
				partExpectedCount = parts.get(partFieldValue);
				partActualCount = 0;
				logger.info("partition " + partFieldName + "=" + partFieldValue +
						" rows=" + partExpectedCount);
				partReader = new BasicTableReader(table);
				partReader.setFilter(new QueryFilter(partFieldName, partFieldValue));
				partReader.addFilter(super.filter);
				partReader.sort = this.sort;
				partReader.viewName = this.viewName;
			}
			else {
				// return an empty RecordList
				chunk = new RecordList(this.table);
				finished = true;
				return chunk;
			}
		}
		chunk = partReader.nextChunk();
		partActualCount += chunk.size();
		recordsRead += chunk.size();
		return chunk;
	}

}
