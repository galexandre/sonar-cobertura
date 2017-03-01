Sonar Cobertura 
===============
[![Build Status](https://travis-ci.org/galexandre/sonar-cobertura.svg?branch=master)](https://travis-ci.org/galexandre/sonar-cobertura)

## Plugin seeking Maintainer(s)
The plugin is not maintained by SonarSource anymore and is consequently seeking for a maintainer from the community. As a logical consequence and as long as no one is maintaining the plugin, any new issue won't be handled.

If you are interested, don't hesitate to contact Sonarsource using our [mailing list](https://groups.google.com/forum/#!forum/sonarqube) or by sending an email directly to **sonarqube[at]googlegroups.com**!

## Description / Feature
This plugin provides the ability to feed SonarQube with code coverage data coming from [Cobertura](http://cobertura.github.io/cobertura/).

Cobertura Plugin | 1.1 | 1.2 | 1.3 | 1.4 | 1.5 | 1.6 | 1.7 | 1.8 
---------------- | --- | --- | --- | --- | --- | --- | --- | --- 
Reports generated with Cobertura | 1.9.4.1	|	1.9.4.1	|	1.9.4.1	|	1.9.4.1	|	1.9.4.1	|	1.9.4.1	|	1.9.4.1 |	1.9.4.1

## Usage
The default location of the XML Cobertura report is: target/site/cobertura/coverage.xml . You can change it in Configure in the Settings > General Settings > Java > Cobertura page

To launch Cobertura from Maven use this command: mvn cobertura:cobertura -Dcobertura.report.format=xml

For more on Cobertura, see [Cobertura' site](http://cobertura.github.io/cobertura/).

See Code [Coverage by Unit Tests for Java Project tutorial](http://docs.sonarqube.org/display/PLUG/Code+Coverage+by+Unit+Tests+for+Java+Project).
