-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-path-expressions.html
SELECT json_query('[ "alpha", 42, "10.4" ]', '$[*].string()'
                  WITH ARRAY WRAPPER)
  FROM dual;