-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-schema.html
CREATE DOMAIN jd AS JSON VALIDATE '{"type" : "object"}';