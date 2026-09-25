-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Using-Extensible-Indexing.html
SELECT last_name, salary FROM employees
   WHERE position_between(salary, 10, 20)=1
   ORDER BY salary DESC, last_name;