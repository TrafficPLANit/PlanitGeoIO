# PLANitGeoIO

![Master Branch](https://github.com/TrafficPLANit/PLANitGeoIO/actions/workflows/maven_master.yml/badge.svg?branch=master)![Develop Branch](https://github.com/TrafficPLANit/PLANitGeoIO/actions/workflows/maven_develop.yml/badge.svg?branch=develop)

Repository allowing one to write PLANit networks to disk in various GIS formats, e.g., shape, geopackage, etc.

## Development

### Maven parent

PLANit Geo-Io has the following PLANit specific dependencies (See pom.xml):

* planit-parentpom
* planit-core
* planit-io
* planit-utils

Dependencies (except parent-pom) will be automatically downloaded from the PLANit website, (www.repository.goplanit.org)[https://repository.goplanit.org], or alternatively can be checked-out locally for local development. The shared PLANit Maven configuration can be found in planit-parent-pom which is defined as the parent pom of each PLANit repository.

### Maven deploy

Distribution management is set up via the parent pom such that Maven deploys this project to the PLANit online repository (also specified in the parent pom). To enable deployment ensure that you setup your credentials correctly in your settings.xml as otherwise the deployment will fail.

## Git Branching model

We adopt GitFlow as per https://nvie.com/posts/a-successful-git-branching-model/
