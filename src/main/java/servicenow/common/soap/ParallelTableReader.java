package servicenow.common.soap;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.Future;

public class ParallelTableReader extends TableReader {

	KeyList keys;
	int numChunks;	
	
	public ParallelTableReader(Table table) throws IOException {
		this(table, null);
	}

	public ParallelTableReader(Table table, QueryFilter filter) throws IOException {
		super(table);
	}
	
	public synchronized KeyList getKeys() 
			throws IOException, InterruptedException {
		if (!this.started) {
			this.started = true;
			KeyReader keyreader = new KeyReader(this.table, this.filter, this.sort);
			this.keys = keyreader.getAllKeys();
			int size = this.keys.size();
			this.numChunks = size / pageSize + (size % pageSize == 0 ? 0 : 1);
			logger.debug("getAllKeys size=" + this.keys.size());			
		}
		return this.keys;
	}
	
	@Override
	public RecordList nextChunk()
			throws IOException, InterruptedException, SoapResponseException, NoSuchElementException {
		throw new UnsupportedOperationException();
	}

	public RecordList getChunk(int index) 
			throws SoapResponseException, IOException, InterruptedException {
		RecordList chunk;
		if (!this.started) getKeys();
		int firstRow = index * pageSize;
		int lastRow = firstRow + pageSize;
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
			chunk = new RecordList(this.table, 0);				
		}
		finished = (lastRow >= keys.size());
		firstRow = lastRow;
		return chunk;	
	}
	
}
