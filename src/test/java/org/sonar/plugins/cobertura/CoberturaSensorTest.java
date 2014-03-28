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

import org.fest.assertions.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.test.IsMeasure;
import org.sonar.api.test.IsResource;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CoberturaSensorTest {

  private SensorContext context;
  private CoberturaSensor sensor;
  private PathResolver pathResolver;
  private Settings settings;
  private JavaResourceLocator javaResourceLocator;
  private Resource resource;

  @Before
  public void setUp() {
    context = mock(SensorContext.class);
    ModuleFileSystem fs = mock(ModuleFileSystem.class);
    pathResolver = mock(PathResolver.class);
    settings = new Settings();
    javaResourceLocator = mock(JavaResourceLocator.class);
    sensor = new CoberturaSensor(fs, pathResolver, settings, javaResourceLocator);
    resource = mock(Resource.class);
  }

  @Test
  public void shouldNotFailIfReportNotSpecifiedOrNotFound() {
    when(pathResolver.relativeFile(any(File.class), anyString()))
        .thenReturn(new File("notFound.xml"));

    Project project = mock(Project.class);

    settings.setProperty(CoberturaPlugin.COBERTURA_REPORT_PATH_PROPERTY, "notFound.xml");
    sensor.analyse(project, context);

    settings.removeProperty(CoberturaPlugin.COBERTURA_REPORT_PATH_PROPERTY);
    sensor.analyse(project, context);
  }

  @Test
  public void doNotCollectProjectCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.COVERAGE), anyDouble());
  }

  @Test
  public void doNotCollectProjectLineCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.LINE_COVERAGE), anyDouble());
    verify(context, never()).saveMeasure(argThat(new IsMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA)));
  }

  @Test
  public void doNotCollectProjectBranchCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.BRANCH_COVERAGE), anyDouble());
  }

  @Test
  public void collectPackageLineCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure((Resource) argThat(is(JavaPackage.class)), eq(CoreMetrics.LINE_COVERAGE), anyDouble());
    verify(context, never()).saveMeasure((Resource) argThat(is(JavaPackage.class)), eq(CoreMetrics.UNCOVERED_LINES), anyDouble());
  }

  @Test
  public void collectPackageBranchCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure((Resource) argThat(is(JavaPackage.class)), eq(CoreMetrics.BRANCH_COVERAGE), anyDouble());
    verify(context, never()).saveMeasure((Resource) argThat(is(JavaPackage.class)), eq(CoreMetrics.UNCOVERED_CONDITIONS), anyDouble());
  }

  @Test
  public void packageCoverageIsCalculatedLaterByDecorator() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure((Resource) argThat(is(JavaPackage.class)), eq(CoreMetrics.COVERAGE), anyDouble());
  }

  @Test
  public void collectFileLineCoverage() throws URISyntaxException {
    when(javaResourceLocator.findResourceByClassName("org.apache.commons.chain.config.ConfigParser")).thenReturn(resource);
    when(context.getResource(any(Resource.class))).thenReturn(resource);
    sensor.parseReport(getCoverageReport(), context);

    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 30.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES, 5.0)));
  }

  @Test
  public void collectFileBranchCoverage() throws URISyntaxException {
    when(javaResourceLocator.findResourceByClassName("org.apache.commons.chain.config.ConfigParser")).thenReturn(resource);
    when(context.getResource(any(Resource.class))).thenReturn(resource);
    sensor.parseReport(getCoverageReport(), context);

    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER, 6.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS, 2.0)));
  }

  @Test
  public void testDoNotSaveMeasureOnResourceWhichDoesntExistInTheContext() throws URISyntaxException {
    when(context.getResource(any(Resource.class))).thenReturn(null);
    sensor.parseReport(getCoverageReport(), context);
    verify(context, never()).saveMeasure(any(Resource.class), any(Measure.class));
  }

  @Test
  public void javaInterfaceHasNoCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    final JavaFile interfaze = new JavaFile("org.apache.commons.chain.Chain");
    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.COVERAGE)));

    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.LINE_COVERAGE)));
    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER)));
    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES)));

    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.BRANCH_COVERAGE)));
    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER)));
    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS)));
  }

  //  @Ignore
  @Test
  public void shouldInsertCoverageAtFileLevel() throws URISyntaxException {
    File coverage = new File(getClass().getResource(
        "/org/sonar/plugins/cobertura/CoberturaSensorTest/shouldInsertCoverageAtFileLevel/coverage.xml").toURI());
    when(resource.getName()).thenReturn("org.sonar.samples.InnerClass");
    when(javaResourceLocator.findResourceByClassName("org.sonar.samples.InnerClass")).thenReturn(resource);
    when(javaResourceLocator.findResourceByClassName("org.sonar.samples.InnerClass$InnerClassInside")).thenReturn(resource);
    when(javaResourceLocator.findResourceByClassName("org.sonar.samples.PrivateClass")).thenReturn(resource);

    when(context.getResource(any(Resource.class))).thenReturn(resource);
    sensor.parseReport(coverage, context);

    verify(context).saveMeasure(eq(resource),
        argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 35.0)));
    verify(context).saveMeasure(eq(resource),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES, 22.0)));

    verify(context).saveMeasure(eq(resource),
        argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER, 4.0)));
    verify(context).saveMeasure(eq(resource),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS, 3.0)));

    verify(context, never()).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.InnerClass$InnerClassInside")),
        argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.InnerClass$InnerClassInside")),
        argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.InnerClass$InnerClassInside")),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.InnerClass$InnerClassInside")),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES)));

    verify(context, never()).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.PrivateClass")),
        argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.PrivateClass")),
        argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.PrivateClass")),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.PrivateClass")),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES)));

    verify(context)
        .saveMeasure(
            eq(resource),
            argThat(new IsMeasure(
                CoreMetrics.COVERAGE_LINE_HITS_DATA,
                "22=2;25=0;26=0;29=0;30=0;31=0;34=1;35=1;36=1;37=0;39=1;41=1;44=2;46=1;47=1;50=0;51=0;52=0;53=0;55=0;57=0;60=0;61=0;64=1;71=1;73=1;76=0;77=0;80=0;81=0;85=0;87=0;91=0;93=0;96=1")));
  }

  @Test
  public void collectFileLineHitsData() throws URISyntaxException {
    when(javaResourceLocator.findResourceByClassName("org.apache.commons.chain.impl.CatalogBase")).thenReturn(resource);
    when(context.getResource(any(Resource.class))).thenReturn(resource);

    sensor.parseReport(getCoverageReport(), context);
    verify(context).saveMeasure(
        eq(resource),
        argThat(new IsMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA,
            "48=117;56=234;66=0;67=0;68=0;84=999;86=999;98=318;111=18;121=0;122=0;125=0;126=0;127=0;128=0;131=0;133=0")));
  }

  @Test
  public void shouldNotCountTwiceAnonymousClasses() throws URISyntaxException {
    File coverage = new File(getClass().getResource("/org/sonar/plugins/cobertura/CoberturaSensorTest/shouldNotCountTwiceAnonymousClasses.xml").toURI());
    when(javaResourceLocator.findResourceByClassName("org.sonar.samples.MyFile")).thenReturn(resource);
    when(context.getResource(any(Resource.class))).thenReturn(resource);
    sensor.parseReport(coverage, context);

    verify(context).saveMeasure(eq(resource), //argThat(new IsResource(Scopes.FILE, Qualifiers.CLASS, "org.sonar.samples.MyFile")),
        argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 5.0))); // do not count line 26 twice
  }

  private File getCoverageReport() throws URISyntaxException {
    return new File(getClass().getResource("/org/sonar/plugins/cobertura/CoberturaSensorTest/commons-chain-coverage.xml").toURI());
  }

  @Test
  public void should_execute_if_report_path_set() throws Exception {
    Project project = mock(Project.class);
    settings.setProperty("sonar.cobertura.reportPath", "coverage.xml");
    Assertions.assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void should_not_execute_if_report_path_not_set() throws Exception {
    Project project = mock(Project.class);
    Assertions.assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }
}
