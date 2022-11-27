-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/XMLAGG.html
SELECT XMLELEMENT("Department",
   XMLAGG(XMLELEMENT("Employee", 
   e.job_id||' '||e.last_name)
   ORDER BY last_name))
   as "Dept_list"     
   FROM employees e
   WHERE e.department_id = 30;