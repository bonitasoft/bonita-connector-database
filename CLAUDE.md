# Database Connector - CLAUDE.md

## Project Overview

**Name:** Database Connector for Bonita
**Artifact:** `org.bonitasoft.connectors:bonita-connector-database`
**Current version:** 2.1.0-SNAPSHOT
**License:** GPL-2.0
**Description:** A Bonita connector that enables Bonita processes to interact with relational databases via JDBC drivers or JNDI DataSources. It supports a wide range of database engines (MySQL, PostgreSQL, Oracle, SQL Server, DB2, H2, HSQLDB, Sybase, Teradata, Informix, Ingres, AS400, MS Access).

---

## Build Commands

The project uses Maven Wrapper (`./mvnw`). Java 11 is required at the source level; CI uses Java 17.

```bash
# Full build (compile, test, package)
./mvnw clean verify

# Skip tests
./mvnw clean package -DskipTests

# Run tests only
./mvnw test

# Build with Sonar analysis (requires SONAR_TOKEN)
./mvnw clean verify sonar:sonar

# Build release artifacts with GPG signing
./mvnw clean verify -P deploy

# Check license headers
./mvnw license:check

# Add missing license headers
./mvnw license:format
```

Build output: `target/*.zip` — one zip assembly per supported database flavour plus an `all` assembly.

---

## Architecture

### Package structure

```
src/main/java/org/bonitasoft/connectors/database/
├── Database.java                    # Core JDBC/DataSource abstraction
├── jdbc/
│   └── JdbcConnector.java           # JDBC-based connector (implements Connector)
└── datasource/
    └── DatasourceConnector.java     # JNDI DataSource-based connector (implements Connector)
```

### Key classes

| Class | Role |
|---|---|
| `Database` | Wraps a `java.sql.Connection` obtained either via `DriverManager` (JDBC URL + driver class) or via JNDI `InitialContext` lookup (DataSource). Provides `select()`, `executeCommand()`, and `executeBatch()`. |
| `JdbcConnector` | Implements Bonita's `Connector` interface. Accepts driver class, URL, credentials, SQL script, optional separator (batch mode), and output type. Supports four output modes: `single`, `one_row`, `n_row`, `table`. |
| `DatasourceConnector` | Implements Bonita's `Connector` interface. Accepts a JNDI datasource name, extra JNDI `Properties`, SQL script, and optional separator (batch mode). Returns raw `ResultSet` for SELECT. |

### Connector descriptor files

Each supported database has a `.properties` descriptor (and i18n variants) under `src/main/resources/`. Filtered resources (`src/main/resources-filtered/`) are expanded by Maven during the build with version/class metadata from `pom.xml` properties.

### Assembly

`src/assembly/` contains one Maven Assembly descriptor per database variant. The `all-assembly.xml` bundles every variant into a single zip.

---

## Testing

Tests are written with **JUnit 5 (Jupiter)** and **AssertJ**. The in-memory **HSQLDB** database is used for integration-style unit tests so no external database is required.

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=JdbcConnectorTest

# Run with coverage report
./mvnw verify   # JaCoCo report at target/site/jacoco/index.html
```

Test sources: `src/test/java/org/bonitasoft/connectors/database/`
- `jdbc/JdbcConnectorTest.java` — tests for `JdbcConnector` using HSQLDB
- `datasource/DatasourceConnectorTest.java` — tests for `DatasourceConnector` using Tomcat JNDI context

External database integration tests (Oracle, MySQL, etc.) are **not** run in CI — they require live database instances and are excluded by default.

### Test coverage

Code coverage is tracked via **JaCoCo** and reported to **SonarCloud** (project key: `bonitasoft_bonita-connector-database`).

---

## Commit Format

This project follows a conventional commit format (used to auto-generate the changelog):

```
type(category): description [flags]

<optional body>
```

Allowed types: `breaking`, `build`, `ci`, `chore`, `docs`, `feat`, `fix`, `other`, `perf`, `refactor`, `revert`, `style`, `test`

Optional flags (square-bracket enclosed): `breaking`

Examples:
```
feat(jdbc): add support for named parameters
fix(datasource): close JNDI context on disconnect
chore(ci): add Claude Code review workflow
docs(readme): update supported databases list
```

Commit message format is enforced by the `commit-message-check.yml` GitHub Actions workflow.

---

## Release Process

Releases are triggered manually via the **Release** GitHub Actions workflow (`workflow_dispatch`):

1. Navigate to **Actions → Release** in the GitHub repository.
2. Click **Run workflow** and enter the release version (e.g., `2.1.0`).
3. The reusable workflow (`bonitasoft/github-workflows/_reusable_release_connector.yml@main`) handles:
   - Version bump in `pom.xml`
   - Git tag creation
   - Maven build with GPG signing (`-P deploy`)
   - Publishing to Maven Central via `central-publishing-maven-plugin`
   - GitHub Release creation with zip artifacts

**Prerequisites for release:**
- `KSM_CONFIG` secret (Keeper Security) must be configured at the org/repo level to inject `SONAR_TOKEN` and GPG credentials.
- `ORGANIZATION_ANTHROPIC_API_KEY` secret must be available at org level for Claude Code workflows.

---

## CI Workflows

| Workflow | Trigger | Purpose |
|---|---|---|
| `build.yml` | push/PR to `master` | Compile, test, SonarCloud analysis, upload artifacts |
| `release.yml` | manual dispatch | Full release to Maven Central |
| `codeql.yml` | scheduled / push | GitHub CodeQL security analysis |
| `commit-message-check.yml` | PR | Enforce commit message format |
| `stale.yml` | scheduled | Mark stale issues/PRs |
| `claude-code-review.yml` | PR open / label / `@claude review` comment | Automated AI code review |
| `claude.yml` | `@claude` mentions in issues/PRs | Interactive Claude Code assistant |
