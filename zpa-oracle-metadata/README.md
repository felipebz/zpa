# zpa-oracle-metadata

Optional module that reads Oracle dictionary metadata (tables, object tables, views and their columns) into the
immutable `ProjectExternalMetadata` used by the ZPA Project System
(`ProjectSnapshot.describeExternalType` and `ProjectSnapshot.narrowCall`).

The module only acquires metadata. It does not validate PL/SQL references, resolve synonyms, follow database links,
or implement Oracle name resolution. Those questions are answered (conservatively) by the Project System from the
metadata you give it. The API is experimental (`@ZpaExperimentalApi`).

## Dependency

```kotlin
dependencies {
    implementation("com.felipebz.zpa:zpa-oracle-metadata:<version>")
    // The Oracle JDBC driver is not a transitive dependency; supply the one you use, for example:
    runtimeOnly("com.oracle.database.jdbc:ojdbc17:23.26.3.0.0")
}
```

The module itself uses only `java.sql`/`javax.sql`.

## Usage

```java
OracleMetadataRequest request = OracleMetadataRequest.builder()
    .schema("HR")                          // whole schema
    .relation("SALES", "CUSTOMERS")        // one relation of another schema
    .queryTimeout(Duration.ofSeconds(30))
    .build();

OracleMetadataResult result = OracleMetadataAcquisition.acquire(dataSource, request);

ProjectExternalMetadata metadata = result.getMetadata();
ProjectIdentifier hr = ProjectIdentifier.fromSource("\"HR\"");   // schema the analyzed unit compiles in
ProjectExternalTypeDescription d = snapshot.describeExternalType(reference, packageOwner, metadata, hr);
```

Names are exact dictionary names (`HR`, `MixedCase`), never quoted or upper-cased by the module.

The result also reports the acquiring session (`sessionUser`, `sessionCurrentSchema`, `database`, `container`),
`acquiredAt`, the schemas declared complete and the requested relations that were not visible. The `currentSchema`
passed to Project System queries is your decision: for stored code it is the owner of the analyzed unit, not
necessarily the acquiring session's schema.

## Connections and transactions

`acquire` takes a connection from your `DataSource` and owns it for the duration of the call.

**Precondition:** the `DataSource` must return an exclusive connection that is not enlisted in a transaction managed
elsewhere, because the acquisition starts and rolls back its own transaction on it. A plain driver `DataSource`
(`OracleDataSource`) or a connection pool handing out idle connections in auto-commit mode is fine. Do not pass a
transaction-aware proxy (Spring `TransactionAwareDataSourceProxy`, a JTA/XA-enlisted container `DataSource`) while a
transaction is active; use a separate, non-transactional `DataSource` for metadata. As a guard, a connection that is
not in auto-commit mode is refused with `IllegalStateException` before anything is executed or changed on it, since
it may carry the caller's uncommitted work.

Then:

1. auto-commit off, then `SET TRANSACTION READ ONLY` as the first statement of the new transaction, so every
   dictionary query sees one consistent state;
2. the dictionary queries (prepared statements, every name bound as a parameter, the request's timeout on each);
3. rollback, auto-commit restored (attempted even if the rollback fails), connection closed.

On any failure the exception propagates (`SQLException`, or `OracleMetadataException` when the rows cannot form valid
descriptors), the transaction is rolled back, the connection is closed, and no result is returned. Continue with
`ProjectExternalMetadata.none()` if analysis should go on without metadata. The returned metadata holds no connection
and is safe to share between threads.

## What is read

| Purpose | View |
|---|---|
| Tables and object tables (row object type from `TABLE_TYPE_OWNER`/`TABLE_TYPE`) | `ALL_ALL_TABLES`, excluding dropped (recycle bin) tables |
| Views | `ALL_VIEWS` |
| Columns | `ALL_TAB_COLS` with `USER_GENERATED = 'YES'`, ordered by `INTERNAL_COLUMN_ID` |
| Session | `SYS_CONTEXT('USERENV', …)` |

The `USER_GENERATED` filter drops system-generated hidden columns (for example the storage columns of `XMLTYPE` and
object-typed columns), which can share a `COLUMN_ID` with a user column. Invisible user columns are kept without a
position, so `%TYPE` resolves them while `%ROWTYPE` excludes them.

Datatypes keep `DATA_TYPE` and `DATA_TYPE_OWNER` verbatim. For named types `DATA_TYPE_OWNER` may reflect how the type
was referenced in the column declaration: on Oracle 26ai `x XMLTYPE` reports `PUBLIC` (the public synonym) and
`x SYS.XMLTYPE` reports `SYS`. Precision and scale are
absent when the dictionary has SQL NULL. A length is given only for character types (in `CHAR_USED` semantics) and
`RAW`; storage lengths of `NUMBER`, `DATE` or LOBs are never reported as declared lengths.

## Whole-schema and targeted acquisition

- `schema(name)` enumerates every table, object table and view of the schema with its columns.
- `relation(schema, name)` reads only the named relations; requests are batched (no query per relation).
- Both can be combined in one request; everything runs in the same read-only transaction.

## Coverage

- Every acquired relation is **column-complete**, because its user-generated columns are queried over the same scope in
  the same transaction (whatever the number of rows): absent columns are reported as `COLUMN_NOT_FOUND`.
- If the dictionary returns the same relation twice, or rows that cannot form valid descriptors, the acquisition fails
  with `OracleMetadataException` instead of returning a partial or arbitrary snapshot.
- A requested schema is declared **complete** only when it is the session user's own schema, where `ALL_*` views are
  not filtered by privileges. Then absent relations are reported as `RELATION_NOT_FOUND`. Note that Oracle may still
  resolve such a name through a synonym; it is not proof that a reference is invalid.
- Any other schema (seen through privileges) and every targeted acquisition stay incomplete: missing relations are
  `NOT_COVERED`/`INCOMPLETE`, never absent. Requested relations that are not visible are listed in `notVisible` and are
  not part of the metadata.
- `DBA_*` views are not used in this version.

## Integration tests

Ordinary builds (`build`, `check`, `test`) need no database. The database-backed tests are a separate suite:

```bash
export ZPA_ORACLE_JDBC_URL='jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
export ZPA_ORACLE_ADMIN_USER=system
export ZPA_ORACLE_ADMIN_PASSWORD=...        # never commit credentials
./gradlew :zpa-oracle-metadata:oracleIntegrationTest
```

They need an account able to create and drop users (for example `system` on Oracle AI Database Free, see
`dev/oracle/README.md` for starting a container with a published port). Each run creates uniquely named users with
random passwords and drops them with `CASCADE` at the end. Without the variables the tests are skipped.
`ZPA_ORACLE_MEASURE=true` additionally runs a timing and memory measurement over 1,200 relations.
