package servicenow.common.soap;

import java.io.IOException;

import servicenow.common.soap.KeyList;
import servicenow.common.soap.Parameters;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.QuerySort;
import servicenow.common.soap.SoapResponseException;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableConfiguration;
import servicenow.common.soap.TableReader;

/**
 * Similar to {@link TableReader}, this class allows keys (sys_ids)
 * to be retrieved in chunks.
 * 
 * @author Giles Lewis
 *
 */
class KeyReader {
	
	final static int DEFAULT_GETKEYS_CHUNKSIZE = 0; 
	final Table table;
	final Parameters baseParams;
	final int chunkSize;
	final boolean checkCount;
	int firstRow = 0;
	boolean done = false;
	Integer expectedCount;
	
	public KeyReader(Table table, QueryFilter filter, QuerySort sort) 
			throws IOException {
		this.table = table;
		baseParams = new Parameters();
		if (filter != null) baseParams.add(filter.asParameters());
		if (sort != null) baseParams.add(sort.asParameters());
		// this parameter can be named either "getkeys_chunk" or "getkeys_limit"
		TableConfiguration config = table.getConfiguration();
		this.chunkSize = config.getInt("getkeys_chunk",
				config.getInt("getkeys_limit",  DEFAULT_GETKEYS_CHUNKSIZE));
		// checkCount adds overhead of an additional Web Services "aggregate" call
		checkCount =
			table.getConfiguration().getBoolean("check_count", true) ||
			table.requestlog.isDebugEnabled() || 
			table.responselog.isDebugEnabled();
		if (checkCount) {
			expectedCount = new Integer(table.getCount(filter));
			table.requestlog.debug("expected count=" + expectedCount);
		}
	}

	protected KeyList nextKeyChunk() 
			throws IOException, SoapResponseException {
		if (done) throw new IllegalStateException();
		Parameters params = new Parameters(baseParams);
		int lastRow = firstRow + chunkSize;
		if (chunkSize > 0) {
			params.add("__first_row", Integer.toString(firstRow));
			params.add("__last_row", Integer.toString(lastRow));
		}
		KeyList chunk = table.getKeys(params);
		if (chunkSize > 0) {
			// This is a kludge.
			// The original code was (chunk.size() < chunkSize).
			// If you ask for 20000 keys and you get 19999 keys
			// then you should be done.
			// However, if there are ACLs in place,
			// then you may get only 19997 keys even though you are not done.
			// So we say that you are done only if you get fewer than 19000 keys.
			// Otherwise you must read again.
			if (chunk.size() < 0.95 * chunkSize) done = true;			
			firstRow = lastRow;
		}
		else
			done = true;
		return chunk;
	}
	
	public KeyList getAllKeys() 
			throws IOException, InterruptedException, SoapResponseException {
		KeyList result = new KeyList();
		int count = 0;
		while (!done) {
			if (Thread.interrupted()) throw new InterruptedException();
			KeyList chunk = nextKeyChunk();
			count += chunk.size();
			if (expectedCount != null && count >= expectedCount) 
				done = true; 
			if (!done) table.responselog.info("getAllKeys " + count + " keys");
			result.addAll(chunk);
		}
		table.responselog.info("getAllKeys " + result.size() + " keys (total)");
		// compare size with expected size
		if (checkCount) {
			if (result.size() != expectedCount)
				// TODO
				// This is a pretty serious issue 
				// but I am not sure how to fix it
				// so for now we will log it and continue.
				table.responselog.error(
					"getAllKeys: list size " + result.size() + 
					" does not equal expected size " + expectedCount +
					" (possible ACL issue)");
		}
		// all keys should be unique
		assert result.size() == result.uniqueCount() :
			"list contains non-unique values";
		return result;
	}
}
