-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-schema.html
CREATE TABLE mytable (
  jcol JSON VALIDATE CAST
       '{"type"       : "object",
         "properties" : {"dateOfBirth" : {"extendedType" : "date"}}}');
INSERT INTO mytable VALUES ('{"dateOfBirth" : "2018-04-11"}');