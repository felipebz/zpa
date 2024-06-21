-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-schema.html
SELECT DBMS_JSON_SCHEMA.validate_report('{"name" : "scott",
                                          "role" : "developer"}',
                                        '{"type" : "array"}')
  AS myreport;