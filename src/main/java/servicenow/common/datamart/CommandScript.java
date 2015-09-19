package servicenow.common.datamart;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import servicenow.common.datamart.CommandBuffer;

public class CommandScript {

	final ArrayList<CommandBuffer> lines = new ArrayList<CommandBuffer>();
	
	CommandScript(File input) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(input));
		// StringBuffer buffer = new StringBuffer();
		String line;
		while ((line = reader.readLine()) != null) {
			// remove comments starting with #
			line = line.replaceFirst("#.*$", "");
			// discard blank lines
			if (line.trim().length() > 0) {
				while (line.endsWith("\\")) {
					line = line.substring(0, line.length() - 1);
					String continuation = reader.readLine();
					continuation = continuation.replaceFirst("#.*$", "");
					line = line + continuation;
				}
				lines.add(new CommandBuffer(line));
				// buffer.append(line + "\n");
			}
		}
		reader.close();
	}

	CommandScript(String input) {
		this(input.split(",\\s*"));		
	}
	
	CommandScript(String[] input) {
		for (int i = 0; i < input.length; ++i)
			lines.add(new CommandBuffer(input[i]));
	}
	
	Iterator<CommandBuffer> iterator() {
		return lines.iterator();
	}
	
}
