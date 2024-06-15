-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SQL-JSON-Conditions.html
SELECT col1
  FROM t
  WHERE col1 IS NOT JSON STRICT AND col1 IS JSON LAX;