-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SQL-JSON-Conditions.html
CREATE DOMAIN jd AS JSON VALIDATE '{"type" : "object"};