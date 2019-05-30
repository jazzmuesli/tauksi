package org.pavelreich.saaremaa.codecov;

import java.util.List;

/**
 * org.jacoco:jacoco-maven-plugin:LATEST:prepare-agent -Dtest=ExampleTest#test1 test rg.jacoco:jacoco-maven-plugin:LATEST:report
 * @author preich
 *
 */
public class MeasureCoverageForIndividualTestsMavenPlugin {

	/**
	 * Identify tests
	 * https://github.com/apache/maven-surefire/blob/master/maven-surefire-plugin/src/main/java/org/apache/maven/plugin/surefire/SurefirePlugin.java
	 * 
	 * scanForTestClasses
	 * executeAfterPreconditionsChecked
	 * https://github.com/apache/maven-surefire/blob/master/maven-surefire-common/src/main/java/org/apache/maven/plugin/surefire/AbstractSurefireMojo.java
	 */
	public <T> List<T> findTests() {
		return null;//TODO
	}
}
