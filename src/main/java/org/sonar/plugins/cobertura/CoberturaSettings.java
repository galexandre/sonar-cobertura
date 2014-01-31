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

import org.sonar.api.BatchExtension;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.plugins.java.api.JavaSettings;

public class CoberturaSettings implements BatchExtension {
  private JavaSettings javaSettings;
  private ModuleFileSystem fileSystem;

  public CoberturaSettings(JavaSettings javaSettings, ModuleFileSystem fileSystem) {
    this.javaSettings = javaSettings;
    this.fileSystem = fileSystem;
  }

  public boolean isEnabled(Project project) {
    return CoberturaPlugin.PLUGIN_KEY.equals(javaSettings.getEnabledCoveragePlugin())
      && !fileSystem.files(FileQuery.onSource().onLanguage(Java.KEY)).isEmpty()
      && project.getAnalysisType().isDynamic(true);
  }
}
