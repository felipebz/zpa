-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-schema.html
CREATE DOMAIN jd AS JSON CONSTRAINT jchkd
  CHECK (jd IS JSON VALIDATE '{"type" : "object"}');