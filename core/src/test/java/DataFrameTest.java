import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Test;
import org.pavelreich.saaremaa.analysis.AnalyseGitHistory.GitResource;
import org.pavelreich.saaremaa.analysis.DataFrame;

public class DataFrameTest {

	@Test
	public void test() {
		Stream<DataFrame> stream = IntStream.range(1, 5).mapToObj(i -> new DataFrame().addColumn("a", i).addColumn("b", i*i));
		List<DataFrame> list = stream.collect(Collectors.toList());
		DataFrame df = list.stream().collect(DataFrame.combine());
		df=df.append(DataFrame.withColumn("c", 93));
		assertEquals(2, list.get(0).nameToValue.size());
		assertEquals(4, list.size());
		assertEquals(4, df.nameToValue.get("a").size());
		System.out.println(df);
		df.toCSV("df.csv");
	}
	
	@Test
	public void testResource() {
		GitResource res = new GitResource("hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/security/TestUGIWithSecurityOn.java","commit",1L,2L);
		assertEquals("org.apache.hadoop.security.TestUGIWithSecurityOn", res.getBaseName());
	}
	@Test
	public void testDifferentColumns() {
		DataFrame df = new DataFrame().addColumn("a", 1).append(new DataFrame().addColumn("b", 3));
		System.out.println(df);
		df.toCSV("a.csv");
	}
}
