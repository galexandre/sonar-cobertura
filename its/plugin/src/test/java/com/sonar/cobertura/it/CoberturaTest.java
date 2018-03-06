/*
 * Cobertura :: Integration Tests
 * Copyright (C) 2018 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.cobertura.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.fest.assertions.Delta;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.client.measure.ComponentWsRequest;

import javax.annotation.CheckForNull;
import java.io.File;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CoberturaTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoberturaTest.class);
    @ClassRule
     public static Orchestrator orchestrator = Orchestrator.builderEnv()
            .addPlugin("java")
            .addPlugin(FileLocation.of("../../target/sonar-cobertura-plugin.jar"))
            .build();

  @Test
  public void shouldReuseCoberturaAndSurefireReports() {
      assertTrue(true);
      MavenBuild build = MavenBuild.create(new File("projects/cobertura-example/pom.xml"));
      if (orchestrator.getConfiguration().getPluginVersion("cobertura").isGreaterThanOrEquals("1.6")) {
          build.setProperty("cobertura.report.format", "xml").setGoals("clean", "cobertura:cobertura", "install"); // cobertura and surefire are NOT executed during build
      } else {
          build.setGoals("clean", "cobertura:cobertura install"); // cobertura and surefire are executed during build
      }
      MavenBuild analysis = MavenBuild.create(new File("projects/cobertura-example/pom.xml"))
              // Do not clean to reuse reports
              .setGoals("sonar:sonar")
              .setProperty("sonar.java.coveragePlugin", "cobertura"); //set this property for java versions 2.1 and prior.
      if (!orchestrator.getConfiguration().getPluginVersion("cobertura").isGreaterThan("1.6.2")) {
         analysis.setProperty("sonar.cobertura.reportPath", "target/site/cobertura/coverage.xml");
      }
      BuildResult[] buildResult = orchestrator.executeBuilds(build, analysis);
      for (int i = 0; i < buildResult.length; i++) {
          assertTrue(buildResult[i].isSuccess());
      }

      Map<String, WsMeasures.Measure> measureMap = getMeasures("com.sonarsource.it.samples:cobertura-example",
             "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests",
            "test_execution_time", "coverage");

      LOGGER.debug("mesureMap size: "+measureMap.size());
      assertNotNull(orchestrator.getServer().getUrl());
      assertNotNull(measureMap);
      if (measureMap!=null){
        if (!orchestrator.getConfiguration().getPluginVersion("cobertura").isGreaterThanOrEquals("1.6")) {
            //no automatic import of surefire information since 1.6
            assertThat(Integer.parseInt(measureMap.get("tests").getValue())).isEqualTo(2);
            assertThat(Integer.parseInt(measureMap.get("test_failures").getValue())).isEqualTo(0);
            assertThat(Integer.parseInt(measureMap.get("test_errors").getValue())).isEqualTo(0);
            assertThat(Integer.parseInt(measureMap.get("skipped_tests").getValue())).isEqualTo(0);
            assertThat(Integer.parseInt(measureMap.get("test_execution_time").getValue())).isGreaterThan(0);
            assertThat(Double.parseDouble(measureMap.get("test_success_density").getValue())).isEqualTo(100.0);
        }
        assertThat(Double.parseDouble(measureMap.get("coverage").getValue())).isEqualTo(57.14285714285714);
    }

  }
    @CheckForNull
    Map<String,WsMeasures.Measure> getMeasures(String componentKey, String... metricKey) {
      return TestUtils.newWsClient(orchestrator).measures().component(new ComponentWsRequest()
              .setComponentKey(componentKey)
              .setMetricKeys(asList(metricKey)))
              .getComponent()
              .getMeasuresList()
              .stream()
              .collect(Collectors.toMap(WsMeasures.Measure::getMetric, Function.identity()));
  }
}
