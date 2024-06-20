-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_BINARY_FLOAT.html
SELECT TO_BINARY_FLOAT('2oo' DEFAULT 0 ON CONVERSION ERROR) "Value"
  FROM DUAL;