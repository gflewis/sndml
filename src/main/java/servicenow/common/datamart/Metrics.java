package servicenow.common.datamart;

import org.slf4j.Logger;

import servicenow.common.datamart.Metrics;
import servicenow.common.soap.FieldValues;
import servicenow.common.soap.Record;

/**
 * Contains counters for the number of records inserted, updated, etc.
 * @author Giles Lewis
 *
 */
public class Metrics {
	
	private int inserts = 0;
	private int updates = 0;
	private int deletes = 0;
	private int unchanged = 0;
	
	private Integer published = null;
	private Integer consumed = null; 
	private Integer expected = null;
	
	public synchronized int recordsInserted()  { return this.inserts; }
	public synchronized int recordsUpdated()   { return this.updates; }
	public synchronized int recordsDeleted()   { return this.deletes; }
	public synchronized int recordsUnchanged() { return this.unchanged; }
	
	public synchronized int recordsPublished() { 
		return published == null ? 0 : published.intValue(); 
	}
	public synchronized Integer recordsConsumed()  { 
		return consumed == null ? 0 : consumed.intValue(); 
	}
	
	public synchronized Integer recordsExpected()  { return this.expected; }
	
	synchronized void incrementInserts(int count)   { this.inserts += count; }	
	synchronized void incrementUpdates(int count)   { this.updates += count; }
	synchronized void incrementDeletes(int count)   { this.deletes += count; }
	synchronized void incrementUnchanged(int count) { this.unchanged += count; }
	
	synchronized void incrementInserts()   { incrementInserts(1); }
	synchronized void incrementUpdates()   { incrementUpdates(1); }
	synchronized void incrementDeletes()   { incrementDeletes(1); }
	synchronized void incrementUnchanged() { incrementUnchanged(1); }
	
	synchronized void incrementPublished(int count) {
		if (published == null) published = new Integer(0);
		published += count;
	}
	
	@Deprecated
	synchronized void incrementConsumed(int count) {
		if (consumed == null) consumed = new Integer(0);
		consumed += count;
	}
	
	synchronized void setExpected(int count) {
		this.expected = new Integer(count);
	}
		
	synchronized Metrics addMetrics(Metrics other) {
		incrementInserts(other.inserts);
		incrementUpdates(other.updates);
		incrementDeletes(other.deletes);
		incrementUnchanged(other.unchanged);
		if (other.published != null) incrementPublished(other.published);
		if (other.consumed != null) incrementConsumed(other.consumed);
		if (other.expected != null) setExpected(other.expected);
		return this;
	}
	
	synchronized void clear() {
		this.inserts = 0;
		this.updates = 0;
		this.deletes = 0;
		this.unchanged = 0;
		this.consumed = null;
		this.published = null;
		this.expected = null;
	}
	
	synchronized void logInfo(Logger logger) {
		logger.info(inserts + " inserts");
		logger.info(updates + " updates");
		if (deletes > 0)   logger.info(deletes + " deletes");
		if (unchanged > 0) logger.info(unchanged + " unchanged");
		if (published != null) logger.info(published + " published");
		// if (consumed != null) logger.info(consumed + " consumed");
		if (expected != null) logger.info(expected + " expected");
	}
	
	synchronized void addValues(Record rec) {
		this.inserts += rec.getInt("u_records_inserted");
		this.updates += rec.getInt("u_records_updated");
		this.deletes += rec.getInt("u_records_deleted");
		this.published += rec.getInt("u_records_processed");
	}
	
	synchronized void loadValues(Record rec) {
		this.inserts = rec.getInt("u_records_inserted");
		this.updates = rec.getInt("u_records_updated");
		this.deletes = rec.getInt("u_records_deleted");
		this.published = rec.getInt("u_records_processed");
	}
	
	synchronized FieldValues fieldValues() {
		FieldValues values = new FieldValues();
		values.set("u_records_inserted", recordsInserted());
		values.set("u_records_updated", recordsUpdated());
		values.set("u_records_deleted", recordsDeleted());
		if (published != null)
			values.set("u_records_processed", published.intValue());
		if (expected != null)
			values.set("u_records_total", this.expected.intValue());
		return values;
	}
}
