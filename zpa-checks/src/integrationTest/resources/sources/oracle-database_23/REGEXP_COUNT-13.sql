-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REGEXP_COUNT.html
SELECT empName, REGEXP_COUNT(empName, 'e', 1, 'c') "CASE_SENSITIVE_E" From regexp_temp;