-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SQL-JSON-Conditions.html
CREATE TABLE tab (jcol JSON VALIDATE '{"type" : "object"}');