# zpa-oracle-metadata

Optional module that reads Oracle dictionary metadata (tables, views and their columns, schema object names and
synonyms) into the immutable `ProjectExternalMetadata` used by the ZPA Project System
(`ProjectSnapshot.describeExternalType` and `ProjectSnapshot.narrowCall`).

The module only acquires metadata: name resolution, synonym resolution and reference checks are done by the Project
System from the metadata you give it. The API is experimental (`@ZpaExperimentalApi`).

## Dependency

```kotlin
dependencies {
    implementation("com.felipebz.zpa:zpa-oracle-metadata:<version>")
    // The Oracle JDBC driver is not a transitive dependency; supply the one you use, for example:
    runtimeOnly("com.oracle.database.jdbc:ojdbc17:23.26.3.0.0")
}
```

## Usage

```java
OracleMetadataRequest request = OracleMetadataRequest.builder()
    .schema("HR")                          // whole schema
    .relation("SALES", "CUSTOMERS")        // one relation of another schema
    .publicSynonyms()                      // every public synonym
    .queryTimeout(Duration.ofSeconds(30))
    .build();

OracleMetadataResult result = OracleMetadataAcquisition.acquire(dataSource, request);

ProjectExternalMetadata metadata = result.getMetadata();
ProjectIdentifier hr = ProjectIdentifier.fromSource("\"HR\"");   // schema the analyzed unit compiles in
ProjectExternalTypeDescription d = snapshot.describeExternalType(reference, packageOwner, metadata, hr);
```

Names are exact dictionary names (`HR`, `MixedCase`). The schema passed to Project System queries is the one the
analyzed unit compiles in, not necessarily the acquiring session's.

The `DataSource` must hand out an exclusive connection outside any transaction managed elsewhere (a plain driver
`DataSource` or a pool in auto-commit mode; not a transaction-aware proxy). All queries run in one read-only
transaction. On failure the exception propagates and no partial result is returned; continue with
`ProjectExternalMetadata.none()` if analysis should go on without metadata.

## What is read

Only `ALL_*` views, so the session sees what its privileges allow:

| Request | Views |
|---|---|
| `schema(name)` | `ALL_ALL_TABLES`, `ALL_VIEWS`, `ALL_TAB_COLS`, `ALL_OBJECTS`, `ALL_SYNONYMS` of that owner |
| `relation(schema, name)` | `ALL_ALL_TABLES`, `ALL_VIEWS`, `ALL_TAB_COLS` of those relations |
| `publicSynonyms()` / `publicSynonymsFor(schema)` | `ALL_SYNONYMS` owned by `PUBLIC` (all, or those pointing into the given schemas) |

## Coverage

Absence is claimed only where the metadata is complete:

- Every acquired relation lists all its columns, so a missing column is `COLUMN_NOT_FOUND`.
- A schema is complete when it is the session user's own schema, or when you assert it with `completeSchema(name)`
  (zpa does not check the session's privileges; a wrong assertion turns relations the session cannot see into
  `RELATION_NOT_FOUND`). In a complete schema, a missing relation or object name is absent; elsewhere it is only not
  covered.
- Public synonyms are complete only with `publicSynonyms()`: every session sees every public synonym. With
  `publicSynonymsFor(...)` they are never complete.
- An unqualified anchor name can be `RELATION_NOT_FOUND` only when the compilation schema and the public synonyms are
  complete.

Requested relations the session cannot see are listed in `OracleMetadataResult.notVisible`.

## Integration tests

Ordinary builds need no database. The database-backed suite:

```bash
export ZPA_ORACLE_JDBC_URL='jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
export ZPA_ORACLE_ADMIN_USER=system
export ZPA_ORACLE_ADMIN_PASSWORD=...        # never commit credentials
./gradlew :zpa-oracle-metadata:oracleIntegrationTest
```

The account must be able to create and drop users (see `dev/oracle/README.md` for a local container); each run
creates its own users and drops them at the end. Without the variables the tests are skipped.
`ZPA_ORACLE_MEASURE=true` also runs a timing and memory measurement.
