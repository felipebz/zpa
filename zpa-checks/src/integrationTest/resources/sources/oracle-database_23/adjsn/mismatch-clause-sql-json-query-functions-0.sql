-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/mismatch-clause-sql-json-query-functions.html
CREATE TYPE person_T AS OBJECT (
  first     VARCHAR2(30),
  last      VARCHAR2(30),
  birthyear NUMBER);