/*
 * SonarQube Cobertura Plugin
 * Copyright (C) 2013 ${owner}
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
package org.sonar.plugins.cobertura;

import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.test.MavenTestUtils;
import org.sonar.plugins.cobertura.base.CoberturaConstants;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CoberturaMavenInitializerTest {

  private Project project;
  private CoberturaMavenInitializer initializer;
  private CoberturaMavenPluginHandler handler;
  private CoberturaSettings coberturaSettings;
  private Settings settings;

  @Before
  public void setUp() {
    project = mock(Project.class);
    handler = mock(CoberturaMavenPluginHandler.class);
    coberturaSettings = mock(CoberturaSettings.class);
    settings = spy(new Settings());
    initializer = new CoberturaMavenInitializer(handler, coberturaSettings, settings);
  }

  @Test
  public void doNotExecuteMavenPluginIfReuseReports() {
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.REUSE_REPORTS);
    assertThat(initializer.getMavenPluginHandler(project)).isNull();
  }

  @Test
  public void doNotExecuteMavenPluginIfStaticAnalysis() {
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.STATIC);
    assertThat(initializer.getMavenPluginHandler(project)).isNull();
  }

  @Test
  public void executeMavenPluginIfDynamicAnalysis() {
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    assertThat(initializer.getMavenPluginHandler(project)).isSameAs(handler);
  }

  @Test
  public void doNotSetReportPathIfAlreadyConfigured() {
    settings.setProperty(CoberturaConstants.COBERTURA_REPORT_PATH_PROPERTY, "foo");
    initializer.execute(project);
    verify(settings, never()).setProperty(eq(CoberturaConstants.COBERTURA_REPORT_PATH_PROPERTY), not(eq("foo")));
  }

  @Test
  public void shouldSetReportPathFromPom() {
    MavenProject pom = MavenTestUtils.loadPom("/org/sonar/plugins/cobertura/CoberturaSensorTest/shouldGetReportPathFromPom/pom.xml");
    when(project.getPom()).thenReturn(pom);
    initializer.execute(project);
    verify(settings).setProperty(eq(CoberturaConstants.COBERTURA_REPORT_PATH_PROPERTY), eq("overridden/dir/coverage.xml"));
  }

  @Test
  public void shouldSetDefaultReportPath() {
    ProjectFileSystem pfs = mock(ProjectFileSystem.class);
    when(pfs.getReportOutputDir()).thenReturn(new File("reportOutputDir"));
    when(project.getFileSystem()).thenReturn(pfs);
    initializer.execute(project);
    verify(settings).setProperty(eq(CoberturaConstants.COBERTURA_REPORT_PATH_PROPERTY), eq("reportOutputDir/cobertura/coverage.xml"));
  }
}
