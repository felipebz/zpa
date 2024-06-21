-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/clauses-used-in-functions-and-conditions-for-json.html
CREATE TYPE person_T AS OBJECT (
  first     VARCHAR2(30),
  last      VARCHAR2(30),
  birthyear NUMBER);