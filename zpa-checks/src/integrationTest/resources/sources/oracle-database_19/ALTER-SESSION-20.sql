-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-SESSION.html
ALTER SESSION SET NLS_CURRENCY = 'FF';

SELECT TO_CHAR( SUM(salary), 'L999G999D99') Total FROM employees;