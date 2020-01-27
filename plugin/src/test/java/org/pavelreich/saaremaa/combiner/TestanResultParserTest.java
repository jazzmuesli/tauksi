package org.pavelreich.saaremaa.combiner;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class TestanResultParserTest {

    @Ignore
    @Test
    public void test() throws IOException {
        Logger logger = LoggerFactory.getLogger(TestanResultParserTest.class);
        TestanResultParser parser = new TestanResultParser(logger);
        Map<String, Metrics> map = new HashMap<>();
        MetricsManager metricsManager = new MetricsManager(map);
        String dir = "/Users/preich/Documents/git/phdp/papers/paper2/sf110";
        parser.doStuff(metricsManager,
                new File(dir+"/43_lilith/src/test/java/result.json"),
                new File(dir+"/43_lilith/src/test/java/class.csv"),
                new File(dir+"/43_lilith/src/test/java/method.csv"));
    }
}