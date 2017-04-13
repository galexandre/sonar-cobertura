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

import com.google.common.collect.Maps;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.StaxParser;
import org.sonar.api.utils.XmlParserException;
import org.sonar.plugins.java.api.JavaResourceLocator;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Map;

import static java.util.Locale.ENGLISH;
import static org.sonar.api.utils.ParsingUtils.parseNumber;

public class CoberturaReportParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(CoberturaReportParser.class);
    
  private final JavaResourceLocator javaResourceLocator;
  private final SensorContext context;

  private CoberturaReportParser(SensorContext context, JavaResourceLocator javaResourceLocator) {
    this.context = context;
    this.javaResourceLocator = javaResourceLocator;
  }

  /**
   * Parse a Cobertura xml report and create measures accordingly
   */
  public static void parseReport(File xmlFile, SensorContext context, JavaResourceLocator javaResourceLocator) {
    new CoberturaReportParser(context, javaResourceLocator).parse(xmlFile);
  }

  private void parse(File xmlFile) {
    try {
      StaxParser parser = new StaxParser(new StaxParser.XmlStreamHandler() {

        @Override
        public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
          rootCursor.advance();
          collectPackageMeasures(rootCursor.descendantElementCursor("package"));
        }
      });
      parser.parse(xmlFile);
    } catch (XMLStreamException e) {
      throw new XmlParserException(e);
    }
  }

  private void collectPackageMeasures(SMInputCursor pack) throws XMLStreamException {
    while (pack.getNext() != null) {
      Map<String, CoverageMeasuresBuilder> builderByFilename = Maps.newHashMap();
      collectFileMeasures(pack.descendantElementCursor("class"), builderByFilename);
      for (Map.Entry<String, CoverageMeasuresBuilder> entry : builderByFilename.entrySet()) {
        String className = sanitizeFilename(entry.getKey());
        InputFile resource = javaResourceLocator.findResourceByClassName(className);
        if (resourceExists(resource)) {
          for (Measure measure : entry.getValue().createMeasures()) {
			try{
				Serializable value = ValueType.DATA.equals(measure.getMetric().getType()) ? measure.getData() : measure.value();
				LOGGER.debug("new measure for metric {} on {}: {}",new Object[] {measure.getMetric(), resource, value});
				context.newMeasure().forMetric(measure.getMetric()).on(resource).withValue(value).save();
			} catch (Exception e) {
				String bad_input = e.getMessage();
				System.out.println("Bad input: " + bad_input);
				continue;
			}
          }
        }
      }
    }
  }

  private boolean resourceExists(InputFile file) {
    return file != null && context.fileSystem().inputFile(context.fileSystem().predicates().is(file.file())) != null;
  }

  private static void collectFileMeasures(SMInputCursor clazz, Map<String, CoverageMeasuresBuilder> builderByFilename) throws XMLStreamException {
    while (clazz.getNext() != null) {
      String fileName = clazz.getAttrValue("filename");
      CoverageMeasuresBuilder builder = builderByFilename.get(fileName);
      if (builder == null) {
        builder = CoverageMeasuresBuilder.create();
        builderByFilename.put(fileName, builder);
      }
      collectFileData(clazz, builder);
    }
  }

  private static void collectFileData(SMInputCursor clazz, CoverageMeasuresBuilder builder) throws XMLStreamException {
    SMInputCursor line = clazz.childElementCursor("lines").advance().childElementCursor("line");
    while (line.getNext() != null) {
      int lineId = Integer.parseInt(line.getAttrValue("number"));
      try {
        builder.setHits(lineId, (int) parseNumber(line.getAttrValue("hits"), ENGLISH));
      } catch (ParseException e) {
        throw new XmlParserException(e);
      }

      String isBranch = line.getAttrValue("branch");
      String text = line.getAttrValue("condition-coverage");
      if (StringUtils.equals(isBranch, "true") && StringUtils.isNotBlank(text)) {
        String[] conditions = StringUtils.split(StringUtils.substringBetween(text, "(", ")"), "/");
        builder.setConditions(lineId, Integer.parseInt(conditions[1]), Integer.parseInt(conditions[0]));
      }
    }
  }

  private static String sanitizeFilename(String s) {
    String fileName = FilenameUtils.removeExtension(s);
    fileName = fileName.replace('/', '.').replace('\\', '.');
    return fileName;
  }

}
