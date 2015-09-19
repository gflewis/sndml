package servicenow.common.datamart;

import java.io.IOException;
import java.sql.SQLException;

import org.jdom2.Document;

import servicenow.common.soap.KeyList;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.Table;
import servicenow.common.soap.XMLFormatter;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import servicenow.common.datamart.DatamartConfiguration;
import servicenow.common.datamart.JobOperation;
import servicenow.common.datamart.LoadMethod;
import servicenow.common.datamart.Message;
import servicenow.common.datamart.Metrics;
import servicenow.common.datamart.ResourceException;
import servicenow.common.datamart.SuiteExecException;
import servicenow.common.datamart.TargetTableWriter;
import servicenow.common.datamart.TargetWriter;

//TODO Complete implementation of this class
/**
 * Implementation of this class was never completed.
 */
@Deprecated
public class QueueTableWriter extends TargetTableWriter {

	Channel channel = null;
	String exchange = null;
	String queueName;
	AMQP.BasicProperties props = null;
		
	public QueueTableWriter(
			TargetWriter connection,
			Table table, 
			String sqlTableName)  {
		super(connection, table, sqlTableName);
		DatamartConfiguration config = 
				DatamartConfiguration.getDatamartConfiguration();
		queueName = config.getRequiredString("queue_name");
	}
	
	void close() {
		try {
		    channel.close();
		    target.close();
		}
		catch (IOException e) {
			throw new ResourceException(e);
		}
	}
	
	void publish(Document doc) throws SuiteExecException {
		String text = XMLFormatter.format(doc);
		try {
			channel.basicPublish(exchange, queueName, props, text.getBytes());
		} catch (IOException e) {
			throw new SuiteExecException(e);
		}
	}
	
	void truncateTable() throws SuiteExecException {
		Message msg = new Message(
				table, sqlTableName, JobOperation.TRUNCATE, null, null);
		publish(msg.asDocument());
	}

	int processRecordSet(RecordList data, LoadMethod method, Metrics metrics)
			throws SuiteExecException {
		Message msg = new Message(
				table, sqlTableName, JobOperation.LOAD, method, data);
		publish(msg.asDocument());
		return 0;
	}

	int deleteRecords(KeyList keys, Metrics metrics) throws SuiteExecException {
		// TODO Auto-generated method stub
		return 0;
	}

	void executeSQL(String command) throws SQLException {
		// TODO Auto-generated method stub		
	}

	void rollback() {
		// TODO Auto-generated method stub		
	}

}
