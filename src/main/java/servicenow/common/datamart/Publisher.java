package servicenow.common.datamart;

import java.io.File;

import org.apache.commons.cli.Options;
import org.slf4j.Logger;

import servicenow.common.datamart.DatamartConfiguration;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.Publisher;

@Deprecated
public class Publisher {
	
	static final Logger logger = LoggerFactory.getLogger(Publisher.class);
	
	public Publisher() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws Exception {

		Options options = new Options();
		options.addOption("p", "prop", true, "Property file (required)");
		options.addOption("f", "script", true, "Specify script file");
		options.addOption("e", true, "Execute a command");
		options.addOption("js", "jobset", true, "Specify name of jobset");
		org.apache.commons.cli.CommandLineParser cparser = 
				new org.apache.commons.cli.BasicParser();
		org.apache.commons.cli.CommandLine line = cparser.parse(options, args);

		String propfilename = line.getOptionValue("p");
		if (propfilename == null) {
			logger.error("Missing propfilename");
			throw new IllegalArgumentException();
		}
		DatamartConfiguration.setPropFile(new File(propfilename));
			
		// config = DatamartConfiguration.getDatamartConfiguration();
	}

}
