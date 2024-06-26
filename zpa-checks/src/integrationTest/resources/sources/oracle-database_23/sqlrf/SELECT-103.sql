-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT t1.department_id, t2.* 
   FROM hr_info t1, TABLE(CAST(MULTISET(
      SELECT t3.last_name, t3.department_id, t3.salary 
         FROM people t3
      WHERE t3.department_id = t1.department_id)
      AS people_tab_typ)) t2;