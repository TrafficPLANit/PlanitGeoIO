# PLANitGeoIO

![Master Branch](https://github.com/TrafficPLANit/PLANitGeoIO/actions/workflows/maven_master.yml/badge.svg?branch=master)![Develop Branch](https://github.com/TrafficPLANit/PLANitGeoIO/actions/workflows/maven_develop.yml/badge.svg?branch=develop)

Repository for PLANit networks to disk in various GIS formats, e.g., shape, geopackage, etc.

## Outputs

Each output is one file per table, a GeoPackage (`.gpkg`, the default) or a shapefile (`.shp` with its companion files). Column names are at most 10 characters, so both formats use the same names. Network and service network files are written per layer and start with `layer_<layer id>_`; conjugate network files start with `conjugate_layer_<layer id>_`. The file names below are the defaults, each can be changed in the settings of its writer.

**Network** (`GeometryNetworkWriter`)

* `planit_nodes`: one point per node
* `planit_links`: one line per link
* `planit_linksegments`: one line per link segment, with per mode columns for banned modes and speeds
* `planit_intersection_nodes`: one row per intersection, with its control type, one true/false column per intersection type (`junction`, `crossing`), the mapped ids of its member nodes, approach segments and internal segments, and a multi point of its member node positions. Written only for layers with intersections.
* `planit_intersection_segments`: one row per approach or internal segment of each intersection, with the id of the intersection (`isect_id`), whether it is an approach (`approach`, false for an internal segment), the control type and intersection types of its intersection, and for approaches the segments it is banned to turn into (`banned_to`). The line runs in the direction of travel, so an approach ends at its intersection. Written only for layers with intersections.

**Zoning** (`GeometryZoningWriter`)

* `planit_zones_od_<geometry type>`, `planit_zones_transfer_<geometry type>`: one file per zone type and geometry type
* `planit_connectoids_od`, `planit_connectoids_transfer`: connectoids
* `planit_connectoid_edges`, `planit_connectoid_segments`: the virtual network connecting zones to the network

**Service network** (`GeometryServiceNetworkWriter`)

* `planit_service_nodes`, `planit_service_legs`, `planit_service_legsegments`

**Routed services** (`GeometryRoutedServicesWriter`)

* `mode_<mode>_planit_service`: one file per layer and mode

The intermodal writer (`GeometryIntermodalWriter`) writes the network, zoning and, optionally, service network and routed services outputs together.

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
