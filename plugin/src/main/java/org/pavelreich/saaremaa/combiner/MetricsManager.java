package org.pavelreich.saaremaa.combiner;

import java.util.Map;

public class MetricsManager {
	Map<String, Metrics> metricsByProdClass;

	public MetricsManager(Map<String, Metrics> metricsByProdClass) {
		this.metricsByProdClass = metricsByProdClass;
	}

	public Metrics provideMetrics(String prodClassName) {
		metricsByProdClass.putIfAbsent(prodClassName, new Metrics(prodClassName));
		Metrics metrics = metricsByProdClass.get(prodClassName);
		return metrics;
	}
}
