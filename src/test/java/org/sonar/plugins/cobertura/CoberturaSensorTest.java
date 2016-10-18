/*
 * SonarQube Cobertura Plugin
 * Copyright (C) 2013-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.cobertura;

import com.google.common.collect.Lists;
import org.fest.assertions.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.test.IsMeasure;
import org.sonar.api.test.IsResource;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;
import java.io.Serializable;
import java.net.URISyntaxException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CoberturaSensorTest {

  private CoberturaSensor sensor;

  private Settings settings;

  @Mock
  private SensorContext context;
  @Mock
  private PathResolver pathResolver;
  @Mock
  private JavaResourceLocator javaResourceLocator;
  @Mock
  private InputFile inputFile;
  @Mock
  private File file;
  @Mock
  private FileSystem fs;
  @Mock
  private FilePredicates predicates;
  @Mock
  private FilePredicate predicate;
  @Mock
  private NewMeasure newMeasure;

  @Before
  public void setUp() {
    initMocks(this);

    settings = new MapSettings();
    sensor = new CoberturaSensor(fs, pathResolver, settings, javaResourceLocator);

    when(context.fileSystem()).thenReturn(fs);
    when(fs.predicates()).thenReturn(predicates);
    when(inputFile.file()).thenReturn(file);
    when(predicates.is(file)).thenReturn(predicate);
    when(fs.inputFile(predicate)).thenReturn(inputFile);

    when(context.newMeasure()).thenReturn(newMeasure);
    when(newMeasure.forMetric(any(Metric.class))).thenReturn(newMeasure);
    when(newMeasure.on(any(InputComponent.class))).thenReturn(newMeasure);
    when(newMeasure.withValue(any())).thenReturn(newMeasure);
  }

  @Test
  public void shouldNotFailIfReportNotSpecifiedOrNotFound() throws URISyntaxException {
    when(pathResolver.relativeFile(any(File.class), anyString()))
        .thenReturn(new File("notFound.xml"));

    settings.setProperty(CoberturaPlugin.COBERTURA_REPORT_PATH_PROPERTY, "notFound.xml");
    sensor.execute(context);


    File report = getCoverageReport();
    settings.setProperty(CoberturaPlugin.COBERTURA_REPORT_PATH_PROPERTY, report.getParent());
    when(pathResolver.relativeFile(any(File.class), anyString()))
        .thenReturn(report.getParentFile().getParentFile());
    sensor.execute(context);
  }

  @Test
  public void doNotCollectProjectCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(newMeasure, never()).forMetric(CoreMetrics.COVERAGE);
  }

  @Test
  public void doNotCollectProjectLineCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(newMeasure, never()).forMetric(CoreMetrics.LINE_COVERAGE);
    verify(newMeasure, never()).forMetric(CoreMetrics.COVERAGE_LINE_HITS_DATA);
  }

  @Test
  public void doNotCollectProjectBranchCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(newMeasure, never()).forMetric(CoreMetrics.BRANCH_COVERAGE);
  }

  @Test
  public void collectPackageLineCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(newMeasure, never()).forMetric(CoreMetrics.LINE_COVERAGE);
    verify(newMeasure, never()).forMetric(CoreMetrics.UNCOVERED_LINES);
  }

  @Test
  public void collectPackageBranchCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(newMeasure, never()).forMetric(CoreMetrics.BRANCH_COVERAGE);
    verify(newMeasure, never()).forMetric(CoreMetrics.UNCOVERED_CONDITIONS);
  }

  @Test
  public void packageCoverageIsCalculatedLaterByDecorator() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    verify(newMeasure, never()).forMetric(CoreMetrics.COVERAGE);
  }

  @Test
  public void collectFileLineCoverage() throws URISyntaxException {
    when(javaResourceLocator.findResourceByClassName("org.apache.commons.chain.config.ConfigParser")).thenReturn(inputFile);
    sensor.parseReport(getCoverageReport(), context);

    verify(context, atLeast(2)).newMeasure();
    verify(newMeasure, atLeast(2)).on(inputFile);
    verify(newMeasure).forMetric(CoreMetrics.LINES_TO_COVER);
    verify(newMeasure).forMetric(CoreMetrics.UNCOVERED_LINES);
    verify(newMeasure).withValue(30);
    verify(newMeasure).withValue(5);
    verify(newMeasure, atLeast(2)).save();
  }

  @Test
  public void collectFileBranchCoverage() throws URISyntaxException {
    when(javaResourceLocator.findResourceByClassName("org.apache.commons.chain.config.ConfigParser")).thenReturn(inputFile);
    sensor.parseReport(getCoverageReport(), context);

    verify(context, atLeast(2)).newMeasure();
    verify(newMeasure, atLeast(2)).on(inputFile);
    verify(newMeasure).forMetric(CoreMetrics.CONDITIONS_TO_COVER);
    verify(newMeasure).forMetric(CoreMetrics.UNCOVERED_CONDITIONS);
    verify(newMeasure).withValue(30);
    verify(newMeasure).withValue(5);
    verify(newMeasure, atLeast(2)).save();
  }

  @Test
  public void testDoNotSaveMeasureOnResourceWhichDoesntExistInTheContext() throws URISyntaxException {
      when(fs.inputFile(predicate)).thenReturn(null);
    sensor.parseReport(getCoverageReport(), context);

    verify(context, never()).newMeasure();
  }

  @Test
  public void javaInterfaceHasNoCoverage() throws URISyntaxException {
    sensor.parseReport(getCoverageReport(), context);

    final InputFile interfaze = new DefaultInputFile("moduleKey", "org/apache/commons/chain/Chain");


    verify(newMeasure, never()).forMetric(CoreMetrics.COVERAGE);

    verify(newMeasure, never()).forMetric(CoreMetrics.LINE_COVERAGE);
    verify(newMeasure, never()).forMetric(CoreMetrics.LINES_TO_COVER);
    verify(newMeasure, never()).forMetric(CoreMetrics.UNCOVERED_LINES);

    verify(newMeasure, never()).forMetric(CoreMetrics.BRANCH_COVERAGE);
    verify(newMeasure, never()).forMetric(CoreMetrics.CONDITIONS_TO_COVER);
    verify(newMeasure, never()).forMetric(CoreMetrics.UNCOVERED_CONDITIONS);
  }

  //  @Ignore
  @Test
  public void shouldInsertCoverageAtFileLevel() throws URISyntaxException {
    File coverage = new File(getClass().getResource(
        "/org/sonar/plugins/cobertura/CoberturaSensorTest/shouldInsertCoverageAtFileLevel/coverage.xml").toURI());
    when(javaResourceLocator.findResourceByClassName("org.sonar.samples.InnerClass")).thenReturn(inputFile);
    when(javaResourceLocator.findResourceByClassName("org.sonar.samples.InnerClass$InnerClassInside")).thenReturn(inputFile);
    when(javaResourceLocator.findResourceByClassName("org.sonar.samples.PrivateClass")).thenReturn(inputFile);

    sensor.parseReport(coverage, context);

    verify(newMeasure, atLeast(1)).on(inputFile);
    verify(newMeasure).forMetric(CoreMetrics.LINES_TO_COVER);
    verify(newMeasure).withValue(35);
    verify(newMeasure).forMetric(CoreMetrics.UNCOVERED_LINES);
    verify(newMeasure).withValue(22);
    verify(newMeasure).forMetric(CoreMetrics.CONDITIONS_TO_COVER);
    verify(newMeasure).withValue(4);
    verify(newMeasure).forMetric(CoreMetrics.UNCOVERED_CONDITIONS);
    verify(newMeasure).withValue(3);

    verify(newMeasure).forMetric(CoreMetrics.COVERAGE_LINE_HITS_DATA);
    verify(newMeasure).withValue("22=2;25=0;26=0;29=0;30=0;31=0;34=1;35=1;36=1;37=0;39=1;41=1;44=2;46=1;47=1;50=0;51=0;52=0;53=0;55=0;57=0;60=0;61=0;64=1;71=1;73=1;76=0;77=0;80=0;81=0;85=0;87=0;91=0;93=0;96=1");
  }

  @Test
  public void collectFileLineHitsData() throws URISyntaxException {
    when(javaResourceLocator.findResourceByClassName("org.apache.commons.chain.impl.CatalogBase")).thenReturn(inputFile);
    sensor.parseReport(getCoverageReport(), context);

    verify(newMeasure).forMetric(CoreMetrics.COVERAGE_LINE_HITS_DATA);
    verify(newMeasure).withValue("48=117;56=234;66=0;67=0;68=0;84=999;86=999;98=318;111=18;121=0;122=0;125=0;126=0;127=0;128=0;131=0;133=0");
  }

  @Test
  public void shouldNotCountTwiceAnonymousClasses() throws URISyntaxException {
    File coverage = new File(getClass().getResource("/org/sonar/plugins/cobertura/CoberturaSensorTest/shouldNotCountTwiceAnonymousClasses.xml").toURI());
    when(javaResourceLocator.findResourceByClassName("org.sonar.samples.MyFile")).thenReturn(inputFile);
    sensor.parseReport(coverage, context);

    verify(newMeasure, atLeast(1)).on(inputFile);
    verify(newMeasure).forMetric(CoreMetrics.LINES_TO_COVER);
    verify(newMeasure).withValue(5); // do not count line 26 twice
  }

  private File getCoverageReport() throws URISyntaxException {
    return new File(getClass().getResource("/org/sonar/plugins/cobertura/CoberturaSensorTest/commons-chain-coverage.xml").toURI());
  }

  @Test
  public void should_execute_only_on_java_files() throws Exception {
    SensorDescriptor descriptor = mock(SensorDescriptor.class);
    when(descriptor.onlyOnLanguage(anyString())).thenReturn(descriptor);
    when(descriptor.onlyOnFileType(any(Type.class))).thenReturn(descriptor);
    sensor.describe(descriptor );
    
    verify(descriptor).onlyOnLanguage("java");
    verify(descriptor).onlyOnFileType(Type.MAIN);
    verify(descriptor).name("CoberturaSensor");
    verifyNoMoreInteractions(descriptor);
  }

}
