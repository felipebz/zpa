-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/generation.html
SELECT json_array(getX(5) FORMAT JSON STRICT) FROM DUAL;