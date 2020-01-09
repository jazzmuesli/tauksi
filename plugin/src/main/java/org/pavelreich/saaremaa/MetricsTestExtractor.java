package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVParser;

public class MetricsTestExtractor implements TestExtractor{

	private MavenLoggerAsSLF4jLoggerAdaptor logger;
	private List<String> srcRoots;

	public MetricsTestExtractor(MavenLoggerAsSLF4jLoggerAdaptor logger, List<String> srcRoots) {
		this.logger = logger;
		this.srcRoots= srcRoots;
	}

	@Override
	public Map<String, Set<String>> extractTestCasesByClass(String path) {
		String fname = path + File.separator + "class.csv";
		logger.info("scanning " + fname);
		try {
			CSVParser parser = Helper.getParser(fname, "class");
			Map<String, Set<String>> ret;
			ret = parser.getRecords().stream().map(x->x.get("class")).filter(p->Helper.isTest(p) && !p.contains("$"))
					.collect(Collectors.toMap(x -> x, x->Collections.emptySet()));
			return ret;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return Collections.emptyMap();
	}

}
