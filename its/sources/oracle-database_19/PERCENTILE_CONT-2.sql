-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/PERCENTILE_CONT.html
SELECT last_name, salary, department_id,
       PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY salary DESC) 
         OVER (PARTITION BY department_id) "Percentile_Cont",
       PERCENT_RANK() 
        OVER (PARTITION BY department_id ORDER BY salary DESC) "Percent_Rank"
  FROM employees
  WHERE department_id IN (30, 60)
  ORDER BY last_name, salary, department_id;