-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-schema.html
CREATE TABLE tab (jcol JSON VALIDATE '{"type" : "object"}');