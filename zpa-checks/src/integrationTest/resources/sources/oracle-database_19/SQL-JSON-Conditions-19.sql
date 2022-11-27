-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SQL-JSON-Conditions.html
SELECT name FROM t
  WHERE JSON_EXISTS(name, '$[*].last');