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

import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.plugins.cobertura.base.CoberturaConstants;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoberturaMavenPluginHandlerTest {

  private CoberturaMavenPluginHandler handler;
  private Settings settings;

  @Before
  public void before() {
    settings = new Settings(new PropertyDefinitions(new CoberturaPlugin().getExtensions().toArray()));
    handler = new CoberturaMavenPluginHandler(settings);
  }

  @Test
  public void users_could_change_version() {
    // first of all, version was fixed : see http://jira.codehaus.org/browse/SONAR-1055
    // but it's more reasonable to let users change the version : see http://jira.codehaus.org/browse/SONAR-1310
    assertThat(handler.isFixedVersion()).isFalse();
  }

  @Test
  public void test_metadata() {
    assertThat(handler.getGroupId()).isEqualTo("org.codehaus.mojo");
    assertThat(handler.getArtifactId()).isEqualTo("cobertura-maven-plugin");
    assertThat(handler.getVersion()).isEqualTo("2.6");
    assertThat(handler.getGoals()).containsOnly("cobertura");
  }

  @Test
  public void should_enable_xml_format() {
    Project project = mock(Project.class);
    when(project.getPom()).thenReturn(new MavenProject());

    MavenPlugin coberturaPlugin = new MavenPlugin(CoberturaConstants.COBERTURA_GROUP_ID, CoberturaConstants.COBERTURA_ARTIFACT_ID, null);
    handler.configure(project, coberturaPlugin);

    assertThat(coberturaPlugin.getParameter("formats/format")).isEqualTo("xml");
  }

  @Test
  public void should_set_max_memory() {
    settings.setProperty(CoberturaConstants.COBERTURA_MAXMEM_PROPERTY, "128m");
    MavenPlugin coberturaPlugin = new MavenPlugin(CoberturaConstants.COBERTURA_GROUP_ID, CoberturaConstants.COBERTURA_ARTIFACT_ID, null);

    Project project = mock(Project.class, Mockito.RETURNS_MOCKS);
    handler.configure(project, coberturaPlugin);

    assertThat(coberturaPlugin.getParameter("maxmem")).isEqualTo("128m");
  }
}
