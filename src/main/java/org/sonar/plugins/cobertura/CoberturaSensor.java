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

import org.slf4j.LoggerFactory;
import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.cobertura.CoberturaReportParserUtils.FileResolver;

import java.io.File;

public class CoberturaSensor implements Sensor, CoverageExtension {

  private CoberturaSettings coberturaSettings;
  private ModuleFileSystem fileSystem;
  private PathResolver pathResolver;
  private Settings settings;

  public CoberturaSensor(CoberturaSettings coberturaSettings, ModuleFileSystem fileSystem, PathResolver pathResolver, Settings settings) {
    this.coberturaSettings = coberturaSettings;
    this.fileSystem = fileSystem;
    this.pathResolver = pathResolver;
    this.settings = settings;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return coberturaSettings.isEnabled(project);
  }

  public void analyse(Project project, SensorContext context) {
    String path = settings.getString(CoberturaConstants.COBERTURA_REPORT_PATH_PROPERTY);
    if (path == null) {
      // wasn't configured - skip
      return;
    }
    File report = pathResolver.relativeFile(fileSystem.baseDir(), path);
    if (!report.exists() || !report.isFile()) {
      LoggerFactory.getLogger(getClass()).warn("Cobertura report not found at {}", report);
      return;
    }
    parseReport(report, context);
  }

  protected void parseReport(File xmlFile, final SensorContext context) {
    LoggerFactory.getLogger(CoberturaSensor.class).info("parsing {}", xmlFile);
    CoberturaReportParserUtils.parseReport(xmlFile, context, new FileResolver() {
      @Override
      public Resource resolve(String filename) {
        return new JavaFile(filename);
      }
    });
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
