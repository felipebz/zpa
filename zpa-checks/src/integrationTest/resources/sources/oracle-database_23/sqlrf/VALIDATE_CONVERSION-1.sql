-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/VALIDATE_CONVERSION.html
SELECT VALIDATE_CONVERSION('$29.99' AS BINARY_FLOAT)
  FROM DUAL;