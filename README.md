Sonar Cobertura 
===============
[![Build Status](https://api.travis-ci.org/SonarQubeCommunity/sonar-cobertura.svg)](https://travis-ci.org/SonarQubeCommunity/sonar-cobertura)

## Description / Feature
This plugin provides the ability to feed SonarQube with code coverage data coming from [Cobertura](http://cobertura.github.io/cobertura/).

Cobertura Plugin | 1.1 | 1.2 | 1.3 | 1.4 | 1.5 | 1.6 | 1.7
---------------- | --- | --- | --- | --- | --- | --- | ---
Reports generated with Cobertura | 1.9.4.1	|	1.9.4.1	|	1.9.4.1	|	1.9.4.1	|	1.9.4.1	|	1.9.4.1	|	1.9.4.1

## Usage
The default location of the XML Cobertura report is: target/site/cobertura/coverage.xml . You can change it in Configure in the Settings > General Settings > Java > Cobertura page

To launch Cobertura from Maven use this command: mvn cobertura:cobertura -Dcobertura.report.format=xml

For more on Cobertura, see [Cobertura' site](http://cobertura.github.io/cobertura/).

See Code [Coverage by Unit Tests for Java Project tutorial](http://docs.sonarqube.org/display/PLUG/Code+Coverage+by+Unit+Tests+for+Java+Project).
