package org.pavelreich.saaremaa.combiner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.bson.Document;

public class Metrics {
	static Set<String> fields = new LinkedHashSet<>();
	public String prodClassName;

	public Map<String, Long> longMetrics = new HashMap<String, Long>();
	Map<String, String> strMetrics = new HashMap<String, String>();
	public String testSessionId = "";
	public String evoSessionId = "";

	public Metrics(String prodClassName) {
		this.prodClassName = prodClassName;
	}

	public void incrementMetric(String metricName) {
		fields.add(metricName);
		longMetrics.putIfAbsent(metricName, 0L);
		longMetrics.put(metricName, longMetrics.get(metricName) + 1);
	}

	public void merge(Map<String, Long> m) {
		fields.addAll(m.keySet());
		longMetrics.putAll(m);
	}
	void put(String metricName, String v) {
		fields.add(metricName);
		strMetrics.put(metricName, v);
	}
	public void put(String metricName, Integer n) {
		put(metricName, Long.valueOf(n));
	}
	
	public void put(String metricName, Long n) {
		fields.add(metricName);
		longMetrics.put(metricName, n);
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public static String[] getFields() {
		List<String> vals = new ArrayList();
		vals.add("prodClassName");
		vals.add("testSessionId");
		vals.add("evoSessionId");
		vals.addAll(fields);
		return vals.toArray(new String[0]);
	}

	public String[] getValues() {
		List<String> vals = new ArrayList();
		vals.add(prodClassName);
		vals.add(testSessionId);
		vals.add(evoSessionId);
		vals.addAll(fields.stream().map(f -> longMetrics.containsKey(f) ? String.valueOf(longMetrics.get(f)) : strMetrics.getOrDefault(f,""))
				.collect(Collectors.toList()));
		return vals.toArray(new String[0]);
	}

	public Document toDocument() {
		Document doc = new Document("prodClassName", prodClassName);
		longMetrics.entrySet().forEach(e -> doc.append(e.getKey().replace('.', '_'), e.getValue()));
		return doc;
	}

}