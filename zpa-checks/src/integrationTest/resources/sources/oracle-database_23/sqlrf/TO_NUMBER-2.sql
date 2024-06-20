-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_NUMBER.html
SELECT TO_NUMBER('2,00' DEFAULT 0 ON CONVERSION ERROR) "Value"
  FROM DUAL;