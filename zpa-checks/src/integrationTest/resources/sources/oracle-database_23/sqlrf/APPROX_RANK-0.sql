-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/APPROX_RANK.html
SELECT job_id, 
			APPROX_SUM(sal), 
      APPROX_RANK(PARTITION BY department_id ORDER BY APPROX_SUM(salary) DESC) 
FROM employees
GROUP BY department_id, job_id
HAVING 
   APPROX_RANK(
   PARTITION BY department_id 
   ORDER BY APPROX_SUM (salary) 
   DESC) <= 10;