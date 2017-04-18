package servicenow.common.soap;

import java.util.concurrent.Callable;

public class ChunkReader implements Callable<RecordList> {

	final Table table;
	final KeyList keys;
	final RecordListProcessor processor;
	
	public ChunkReader(Table table, KeyList keys, RecordListProcessor processor) {
		assert table != null;
		assert keys != null;
		assert processor != null;
		this.table = table;
		this.keys = keys;
		this.processor = processor;
	}

	@Override
	public RecordList call() throws Exception {
		QueryFilter keyfilter = keys.queryFilter();
		Parameters params = new Parameters(keyfilter.asParameters());
		if (Thread.interrupted()) throw new InterruptedException();
		RecordList chunk = table.getRecords(params);
		processor.processList(chunk);
		return chunk;
	}

}
