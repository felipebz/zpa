-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/REGEXP_COUNT.html
SELECT empName, REGEXP_COUNT(empName, 'E', 1, 'i') "CASE_INSENSITIVE_E" From regexp_temp;