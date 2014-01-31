/*
 * SonarQube Cobertura Plugin
 * Copyright (C) 2013 SonarSource
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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.plugins.java.api.JavaSettings;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoberturaSettingsTest {

  private Settings settings;
  private JavaSettings javaSettings;
  private ModuleFileSystem fileSystem;
  private Project javaProject;
  private CoberturaSettings coberturaSettings;

  @Before
  public void before() {
    settings = new Settings(new PropertyDefinitions(new CoberturaPlugin().getExtensions().toArray()));
    javaSettings = mock(JavaSettings.class);
    when(javaSettings.getEnabledCoveragePlugin()).thenReturn("cobertura");
    fileSystem = mock(ModuleFileSystem.class);
    when(fileSystem.files(any(FileQuery.class))).thenReturn(Arrays.asList(new File("")));
    javaProject = mock(Project.class);
    when(javaProject.getLanguageKey()).thenReturn(Java.KEY);
    when(javaProject.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    coberturaSettings = new CoberturaSettings(javaSettings, fileSystem);
  }

  @Test
  public void should_be_enabled_if_project_with_java_sources() {
    assertThat(coberturaSettings.isEnabled(javaProject)).isTrue();
  }

  @Test
  public void should_be_disabled_if_java_project_without_sources() {
    when(fileSystem.files(any(FileQuery.class))).thenReturn(Collections.<File> emptyList());
    assertThat(coberturaSettings.isEnabled(javaProject)).isFalse();
  }

  @Test
  public void should_be_disabled_if_static_analysis_only() {
    when(javaProject.getAnalysisType()).thenReturn(Project.AnalysisType.STATIC);
    assertThat(coberturaSettings.isEnabled(javaProject)).isFalse();
  }

  @Test
  public void should_be_enabled_if_reuse_report_mode() {
    when(javaProject.getAnalysisType()).thenReturn(Project.AnalysisType.REUSE_REPORTS);
    assertThat(coberturaSettings.isEnabled(javaProject)).isTrue();
  }

  /**
   * http://jira.codehaus.org/browse/SONAR-2897: there used to be a typo in the parameter name (was "sonar.cobertura.maxmen")
   */
  @Test
  public void should_support_deprecated_max_memory() {
    settings.setProperty("sonar.cobertura.maxmen", "128m");
    assertThat(settings.getString("sonar.cobertura.maxmem")).isEqualTo("128m");
  }
}
