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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;
import java.net.URISyntaxException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.plugins.java.Java;

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
  private NewCoverage newCoverage;

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

    when(context.newCoverage()).thenReturn(newCoverage);
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
  public void collectFileLineCoverage() throws URISyntaxException {
    when(javaResourceLocator.findResourceByClassName("org.apache.commons.chain.config.ConfigParser")).thenReturn(inputFile);
    sensor.parseReport(getCoverageReport(), context);

    verify(context, times(1)).newCoverage();
    verify(newCoverage, times(1)).onFile(inputFile);
    verify(newCoverage).lineHits(162,27);
    verify(newCoverage).lineHits(77,60);
    verify(newCoverage, times(1)).save();
  }

  @Test
  public void collectFileBranchCoverage() throws URISyntaxException {
    when(javaResourceLocator.findResourceByClassName("org.apache.commons.chain.config.ConfigParser")).thenReturn(inputFile);
    sensor.parseReport(getCoverageReport(), context);

    verify(context, times(1)).newCoverage();
    verify(newCoverage, times(1)).onFile(inputFile);
    verify(newCoverage).conditions(73, 2, 1);
    verify(newCoverage).conditions(93, 2, 2);
    verify(newCoverage, times(1)).save();
  }

  @Test
  public void testDoNotSaveMeasureOnResourceWhichDoesntExistInTheContext() throws URISyntaxException {
    when(fs.inputFile(predicate)).thenReturn(null);
    sensor.parseReport(getCoverageReport(), context);

    verify(context, never()).newCoverage();
  }

  @Test
  public void javaInterfaceHasNoCoverageSoAddedAFakeOneToShowAsCovered() throws URISyntaxException {
    when(javaResourceLocator.findResourceByClassName("org.apache.commons.chain.Chain")).thenReturn(inputFile);    
    sensor.parseReport(getCoverageReport(), context);

    verify(newCoverage, times(1)).onFile(inputFile);
    verify(newCoverage).lineHits(1,1);
    verify(newCoverage, times(1)).save();
  }

  @Test
  public void shouldInsertCoverageAtFileLevel() throws URISyntaxException {
    File coverage = new File(getClass().getResource(
        "/org/sonar/plugins/cobertura/CoberturaSensorTest/shouldInsertCoverageAtFileLevel/coverage.xml").toURI());
    when(javaResourceLocator.findResourceByClassName("org.sonar.samples.InnerClass")).thenReturn(inputFile);
    when(javaResourceLocator.findResourceByClassName("org.sonar.samples.InnerClass$InnerClassInside")).thenReturn(inputFile);
    when(javaResourceLocator.findResourceByClassName("org.sonar.samples.PrivateClass")).thenReturn(inputFile);

    sensor.parseReport(coverage, context);

    verify(newCoverage, times(3)).onFile(inputFile);

    verify(newCoverage).lineHits(22,2);
    verify(newCoverage).lineHits(44,2);

    verify(newCoverage).lineHits(25,0);
    verify(newCoverage).lineHits(26,0);

    verify(newCoverage).lineHits(34,1);
    verify(newCoverage).lineHits(35,1);
    verify(newCoverage).lineHits(36,1);
    verify(newCoverage).conditions(36, 2, 1);

    verify(newCoverage).lineHits(37,0);
    verify(newCoverage).lineHits(39,1);
    verify(newCoverage).lineHits(41,1);

    verify(newCoverage).lineHits(29,0);
    verify(newCoverage).lineHits(30,0);
    verify(newCoverage).lineHits(31,0);


    verify(newCoverage).lineHits(46,1);
    verify(newCoverage).lineHits(47,1);

    verify(newCoverage).lineHits(50,0);
    verify(newCoverage).lineHits(51,0);
    verify(newCoverage).lineHits(52,0);
    verify(newCoverage).conditions(52, 2, 0);

    verify(newCoverage).lineHits(53,0);
    verify(newCoverage).lineHits(55,0);
    verify(newCoverage).lineHits(57,0);


    verify(newCoverage).lineHits(60,0);
    verify(newCoverage).lineHits(61,0);
    verify(newCoverage).lineHits(64,1);


    verify(newCoverage).lineHits(71,1);
    verify(newCoverage).lineHits(73,1);

    verify(newCoverage).lineHits(85,0);
    verify(newCoverage).lineHits(87,0);

    verify(newCoverage).lineHits(80,0);
    verify(newCoverage).lineHits(81,0);

    verify(newCoverage).lineHits(91,0);
    verify(newCoverage).lineHits(93,0);

    verify(newCoverage).lineHits(76,0);
    verify(newCoverage).lineHits(77,0);

    verify(newCoverage).lineHits(96,1);

    verify(newCoverage, times(3)).save();

    verifyNoMoreInteractions(newCoverage);
  }

  @Test
  public void collectFileLineHitsData() throws URISyntaxException {
    when(javaResourceLocator.findResourceByClassName("org.apache.commons.chain.impl.CatalogBase")).thenReturn(inputFile);
    sensor.parseReport(getCoverageReport(), context);

    verify(newCoverage, times(1)).onFile(inputFile);
    verify(newCoverage).lineHits(56,234);

    verify(newCoverage).lineHits(48,117);
    verify(newCoverage).lineHits(66,0);
    verify(newCoverage).lineHits(67,0);
    verify(newCoverage).lineHits(68,0);

    verify(newCoverage).lineHits(84,999);
    verify(newCoverage).lineHits(86,999);

    verify(newCoverage).lineHits(98,318);

    verify(newCoverage).lineHits(111,18);
    
    verify(newCoverage).lineHits(121,0);
    verify(newCoverage).lineHits(122,0);
    verify(newCoverage).lineHits(125,0);
    verify(newCoverage).conditions(125, 2, 0);

    verify(newCoverage).lineHits(126,0);
    verify(newCoverage).lineHits(127,0);
    verify(newCoverage).conditions(127, 2, 0);
    verify(newCoverage).lineHits(128,0);
    verify(newCoverage).lineHits(131,0);
    verify(newCoverage).lineHits(133,0);

    verify(newCoverage, times(1)).save();

    verifyNoMoreInteractions(newCoverage);
  }

  @Test
  public void countsLineNumbersGloballyEvenAnonymousClasses() throws URISyntaxException {
    File coverage = new File(getClass().getResource("/org/sonar/plugins/cobertura/CoberturaSensorTest/shouldNotCountTwiceAnonymousClasses.xml").toURI());
    when(javaResourceLocator.findResourceByClassName("org.sonar.samples.MyFile")).thenReturn(inputFile);
    sensor.parseReport(coverage, context);

    verify(newCoverage, times(2)).onFile(inputFile);
    verify(newCoverage).lineHits(22,2);
    verify(newCoverage).lineHits(25,0);
    verify(newCoverage, times(2)).lineHits(26,0);
    verify(newCoverage).lineHits(27,0);
    verify(newCoverage).lineHits(28,0);

    verify(newCoverage, times(2)).save();

    verifyNoMoreInteractions(newCoverage);

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
    
    verify(descriptor).onlyOnLanguage(Java.KEY);
    verify(descriptor).onlyOnFileType(Type.MAIN);
    verify(descriptor).name("CoberturaSensor");
    verifyNoMoreInteractions(descriptor);
  }

}
