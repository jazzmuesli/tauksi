package org.pavelreich.saaremaa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * Wrapper for final CSVPrinter (that's why people need Powermock!).
 * 
 * @author preich
 *
 */
public class CSVReporter implements AutoCloseable {

	private CSVPrinter csvPrinter;

	public CSVReporter(String fname, String... fields) throws IOException {
		this(new CSVPrinter(Files.newBufferedWriter(Paths.get(fname)),
				CSVFormat.DEFAULT.withHeader(fields).withSystemRecordSeparator().withDelimiter(';')));
	}

	public CSVReporter(CSVPrinter csvPrinter) {
		this.csvPrinter = csvPrinter;
	}

	public synchronized void write(Object... values) {
		try {
			csvPrinter.printRecord(values);
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	public synchronized void flush() {
		try {
			csvPrinter.flush();
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	@Override
	public synchronized void close() {
		try {
			csvPrinter.close();
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

}