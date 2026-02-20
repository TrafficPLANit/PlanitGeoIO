# Release Log

This project contains code to write networks to disk in various GIS formats based on PLANit memory model

## 0.5.0

**Enhancements**

#2 (partial) When specified output directory does not exist yet, create it rather than failing
#7 When a link has no internal geometry we now approximate it by creating a line between the two nodes at the extremities
#8 (prototype) Baseline support for persisting a conjugate network
#9 (partial) Add support for geopackage format in addition to shape and make it the default (because it is smaller)
[GENERAL] migrate to newer version of geotools

**Bug fixes**

#5 Fixed an issue where getting started example shape .prj file was empty
#6 Fixed an issue where geometries were not correct converted to explicitly set CRS 

## 0.4.0

* First implementation to support GIS (shape files) writers for all PLANit components and intermodal version (except demands)
