package org.pavelreich.saaremaa.analysis;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.bson.Document;
import org.pavelreich.saaremaa.CSVReporter;

/**
 * An untyped dataframe.
 * 
 * new DataFrame().addColumn("a",3).toCSV("df.csv");
 * 
 * @author preich
 *
 */
public class DataFrame {
	public Map<String, List> nameToValue = new HashMap<String, List>();
	private Object defValue = "0";

	public <V> DataFrame addColumn(String name, V value) {
		List<V> ls = createList();
		ls.add(value);
		nameToValue.put(name, ls);
		return this;
	}

	public DataFrame append(final DataFrame df) {
		df.nameToValue.keySet().forEach(k -> {
			this.nameToValue.putIfAbsent(k, createDefaultList());
			List ourValues = this.nameToValue.get(k);
			ourValues.addAll(df.nameToValue.getOrDefault(k, createDefaultList()));
		});
		return this;
	}

	private List createDefaultList() {
		return getRowsStream().mapToObj(i -> defaultValue()).collect(Collectors.toList());
	}

	public String toString() {
		StringBuilder str = new StringBuilder(nameToValue.keySet().stream().collect(Collectors.joining("|")));
		getRowsStream().forEach(i -> {
			String s = nameToValue.keySet().stream().map(k -> String.valueOf(getValue(i, k)))
					.collect(Collectors.joining("|"));
			str.append("\n").append(s);
		});
		return str.toString();

	}

	public DataFrame() {
	}

	DataFrame(String defValue) {
		this.defValue = defValue;
	}

	public Object defaultValue() {
		return defValue;
	}

	private <V> List<V> createList() {
		return new LinkedList<V>();
	}

	public static Collector<DataFrame, DataFrame, DataFrame> combine() {
		return Collector.<DataFrame, DataFrame>of(() -> new DataFrame(), (df1, df2) -> df1.append(df2),
				(df1, df2) -> df1.append(df2), Characteristics.IDENTITY_FINISH);
	}

	public int rows() {
		return nameToValue.size() == 0 ? 0 : nameToValue.values().iterator().next().size();
	}

	public void toCSV(String fname) {
		String[] cols = getColumns();
		try (CSVReporter reporter = new CSVReporter(fname, cols)) {
			getRowsStream().forEach(i -> {
				Object[] vals = Arrays.asList(cols).stream().map(col -> getValue(i, col)).toArray();
				reporter.write(vals);
			});
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	private Object getValue(int i, String col) {
		return nameToValue.get(col).get(i);
	}

	private IntStream getRowsStream() {
		return IntStream.range(0, rows());
	}

	private String[] getColumns() {
		String[] cols = nameToValue.keySet().toArray(new String[0]);
		return cols;
	}

	public static <V> DataFrame withColumn(String name, V value) {
		return new DataFrame().addColumn(name, value);
	}

	public List<Document> toDocuments() {
		Stream<Document> documents = getRowsStream().mapToObj(i -> toDocument(i));
		return documents.collect(Collectors.toList());
	}

	public Document toDocument(int row) {
		Document d = new Document();
		Set<String> cols = nameToValue.keySet();
		for (String col : cols) {
			d = d.append(col, getValue(row, col));
		}
		return d;
	}
}
