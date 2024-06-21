-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-in-oracle-database.html
SELECT json_value(json_scalar(current_timestamp), '$.timestamp()')
  FROM DUAL;