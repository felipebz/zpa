-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/type-clause-sql-functions-and-conditions.html
ALTER SESSION SET JSON_BEHAVIOR=
  "lax_json_value:false;lax_json_query:false;lax_json_table:false;lax_json_exists:false";