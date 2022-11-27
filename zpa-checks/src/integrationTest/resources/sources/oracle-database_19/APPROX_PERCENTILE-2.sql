-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/APPROX_PERCENTILE.html
SELECT department_id "Department",
       APPROX_PERCENTILE(0.25 DETERMINISTIC, 'CONFIDENCE')
         WITHIN GROUP (ORDER BY salary ASC) "Confidence"
FROM employees
GROUP BY department_id
ORDER BY department_id; 