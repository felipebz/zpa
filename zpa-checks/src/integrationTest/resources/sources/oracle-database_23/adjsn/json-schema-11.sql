-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-schema.html
CREATE TABLE tab (jcol JSON CONSTRAINT jchk
  CHECK (jcol IS JSON VALIDATE '{"type" : "object"}'));
CREATE TABLE jtab(jcol JSON DOMAIN jd);
CREATE DOMAIN jd AS JSON VALIDATE '{"type" : "object"}';
SELECT * FROM USER_JSON_SCHEMA_COLUMNS;