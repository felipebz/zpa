-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-SESSION.html
ALTER SESSION
   SET NLS_ISO_CURRENCY = America; 

SELECT TO_CHAR( SUM(salary), 'C999G999D99') Total
   FROM employees; 