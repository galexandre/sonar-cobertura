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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.CoreProperties;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;

public class CoberturaSensor implements Sensor {

  private static final Logger LOGGER = LoggerFactory.getLogger(CoberturaSensor.class);

  private FileSystem fs;
  private PathResolver pathResolver;
  private Settings settings;
  private final JavaResourceLocator javaResourceLocator;

  public CoberturaSensor(FileSystem fs, PathResolver pathResolver, Settings settings, JavaResourceLocator javaResourceLocator) {
    this.fs = fs;
    this.pathResolver = pathResolver;
    this.settings = settings;
    this.javaResourceLocator = javaResourceLocator;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(CoreProperties.CATEGORY_JAVA).onlyOnFileType(Type.MAIN).name("CoberturaSensor");
  }

  @Override
  public void execute(SensorContext context) {
    String path = settings.getString(CoberturaPlugin.COBERTURA_REPORT_PATH_PROPERTY);
    File report = pathResolver.relativeFile(fs.baseDir(), path);
    if (!report.isFile() || !report.exists() || !report.canRead()) {
      LOGGER.warn("Cobertura report not found at {}", report);
    } else {
      parseReport(report, context);
    }
  }

  protected void parseReport(File xmlFile, SensorContext context) {
    LOGGER.info("parsing {}", xmlFile);
    CoberturaReportParser.parseReport(xmlFile, context, javaResourceLocator);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
