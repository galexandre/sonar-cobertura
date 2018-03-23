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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.java.api.JavaResourceLocator;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.text.ParseException;

import static java.util.Locale.ENGLISH;
import javax.xml.stream.XMLInputFactory;
import org.codehaus.staxmate.SMInputFactory;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
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
            SMInputFactory inputFactory = initStax();

            SMHierarchicCursor rootCursor = inputFactory.rootElementCursor(xmlFile);
            while (rootCursor.getNext() != null) {
                collectPackageMeasures(rootCursor.descendantElementCursor("package"));
            }
            rootCursor.getStreamReader().closeCompletely();
        }
        catch (XMLStreamException e) {
            throw new IllegalStateException("XML is not valid", e);
        }
    }

    private static SMInputFactory initStax() {
        final XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
        xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        return new SMInputFactory(xmlFactory);
    }

    private void collectPackageMeasures(SMInputCursor pack) throws XMLStreamException {
        while (pack.getNext() != null) {
            collectFileMeasures(pack.descendantElementCursor("class"));
        }
    }

    private boolean resourceExists(InputFile file) {
        return file != null && context.fileSystem().inputFile(context.fileSystem().predicates().is(file.file())) != null;
    }

    private void collectFileMeasures(SMInputCursor clazz) throws XMLStreamException {
        while (clazz.getNext() != null) {
            String fileName = clazz.getAttrValue("filename");
            collectFileData(clazz, fileName);
        }
    }

    private void collectFileData(SMInputCursor clazz, String filename) throws XMLStreamException {
        String className = sanitizeFilename(filename);
        InputFile resource = javaResourceLocator.findResourceByClassName(className);
        NewCoverage coverage = null;
        boolean lineAdded = false;
        if (resourceExists(resource)) {
            coverage = context.newCoverage();
            coverage.onFile(resource);
        }

        SMInputCursor line = clazz.childElementCursor("lines").advance().childElementCursor("line");
        while (line.getNext() != null) {
            int lineId = Integer.parseInt(line.getAttrValue("number"));
            try {
                if (coverage != null) {
                    coverage.lineHits(lineId, (int) parseNumber(line.getAttrValue("hits"), ENGLISH));
                    lineAdded = true;
                }
            }
            catch (ParseException e) {
                throw new XMLStreamException(e);
            }

            String isBranch = line.getAttrValue("branch");
            String text = line.getAttrValue("condition-coverage");
            if (StringUtils.equals(isBranch, "true") && StringUtils.isNotBlank(text)) {
                String[] conditions = StringUtils.split(StringUtils.substringBetween(text, "(", ")"), "/");
                if (coverage != null) {
                    coverage.conditions(lineId, Integer.parseInt(conditions[1]), Integer.parseInt(conditions[0]));
                    lineAdded = true;
                }
            }
        }
        if (coverage != null) {
            // If there was no lines covered or uncovered (e.g. everything is ignored), but the file exists then Sonar would report the file as uncovered
            // so adding a fake one to line number 1
            if (!lineAdded) {
                coverage.lineHits(1, 1);
            }
            coverage.save();
        }
    }

    private static String sanitizeFilename(String s) {
        String fileName = FilenameUtils.removeExtension(s);
        fileName = fileName.replace('/', '.').replace('\\', '.');
        return fileName;
    }

}
