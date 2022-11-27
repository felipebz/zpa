-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SYS_XMLAGG.html
SELECT SYS_XMLAGG(SYS_XMLGEN(last_name)) XMLAGG
   FROM employees
   WHERE last_name LIKE 'R%'
   ORDER BY xmlagg;