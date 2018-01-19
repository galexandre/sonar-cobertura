/*
 * Cobertura :: Integration Tests
 * Copyright (C) 2009 SonarSource
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
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.fest.assertions.Delta;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class CoberturaTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .setOrchestratorProperty("javaVersion", "4.11").addPlugin("java")
    .addPlugin(FileLocation.of("../../target/sonar-cobertura-plugin.jar"))
    .build();

  @Test
  public void shouldReuseCoberturaAndSurefireReports() {
    MavenBuild build = MavenBuild.create(new File("projects/cobertura-example/pom.xml"));
    if (orchestrator.getConfiguration().getPluginVersion("cobertura").isGreaterThanOrEquals("1.6")) {
      build.setProperty("cobertura.report.format", "xml").setGoals("clean", "cobertura:cobertura", "install"); // cobertura and surefire are NOT executed during build
    } else {
      build.setGoals("clean", "install"); // cobertura and surefire are executed during build
    }
    MavenBuild analysis = MavenBuild.create(new File("projects/cobertura-example/pom.xml"))
        // Do not clean to reuse reports
        .setGoals("sonar:sonar")
        .setProperty("sonar.java.coveragePlugin", "cobertura"); //set this property for java versions 2.1 and prior.
    if (!orchestrator.getConfiguration().getPluginVersion("cobertura").isGreaterThan("1.6.2")) {
      analysis.setProperty("sonar.cobertura.reportPath", "target/site/cobertura/coverage.xml");
    }
    orchestrator.executeBuilds(build, analysis);
    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:cobertura-example",
        "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time", "coverage"));
    if (!orchestrator.getConfiguration().getPluginVersion("cobertura").isGreaterThanOrEquals("1.6")) {
      //no automatic import of surefire information since 1.6
      assertThat(project.getMeasureIntValue("tests")).isEqualTo(2);
      assertThat(project.getMeasureIntValue("test_failures")).isEqualTo(0);
      assertThat(project.getMeasureIntValue("test_errors")).isEqualTo(0);
      assertThat(project.getMeasureIntValue("skipped_tests")).isEqualTo(0);
      assertThat(project.getMeasureIntValue("test_execution_time")).isGreaterThan(0);
      assertThat(project.getMeasureValue("test_success_density")).isEqualTo(100.0);
    }
    assertThat(project.getMeasureValue("coverage")).isEqualTo(57.1, Delta.delta(0.1));
  }

}
