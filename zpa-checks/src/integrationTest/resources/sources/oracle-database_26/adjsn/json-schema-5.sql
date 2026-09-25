-- https://docs.oracle.com/en/database/oracle/oracle-database/26/adjsn/json-schema.html
CREATE DOMAIN jd AS JSON VALIDATE '{"type" : "object"}';