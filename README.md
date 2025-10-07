# bonita-connector-database
![](https://github.com/bonitasoft/bonita-connector-database/workflows/Build/badge.svg)
[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=bonitasoft_bonita-connector-database&metric=alert_status)](https://sonarcloud.io/dashboard?id=bonitasoft_bonita-connector-database)
[![GitHub release](https://img.shields.io/github/v/release/bonitasoft/bonita-connector-database?color=blue&label=Release)](https://github.com/bonitasoft/bonita-connector-database/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.bonitasoft.connectors/bonita-connector-database.svg?label=Maven%20Central&color=orange)](https://search.maven.org/search?q=g:%22org.bonitasoft.connectors%22%20AND%20a:%22bonita-connector-database%22)
[![License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-yellow.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)

JDBC and Datasource database connectors for Bonita

## Build

__Clone__ or __fork__ this repository, then at the root of the project run:

`./mvnw`

## Release

In order to create a new release: 
- On the release branch, make sure to update the pom version (remove the -SNAPSHOT)
- Run the [release](https://github.com/bonitasoft/bonita-connector-database/actions/workflows/release.yml) action, set the version to release as parameter
- Update the `master` with the next SNAPSHOT version.

Once this is done, update the [Bonita marketplace repository](https://github.com/bonitasoft/bonita-marketplace) with the new version of the connector.

## Contributing

We would love you to contribute, pull requests are welcome! Please see the [CONTRIBUTING.md](CONTRIBUTING.md) for more information.

## License

The sources and documentation in this project are released under the [GPLv2 License](LICENSE)
