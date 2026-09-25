-- https://docs.oracle.com/en/database/oracle/oracle-database/26/adjsn/json-data-type.html
SELECT json_value(json_scalar(current_timestamp), '$.timestamp()');