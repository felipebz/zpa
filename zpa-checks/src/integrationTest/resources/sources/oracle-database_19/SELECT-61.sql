-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SELECT.html
CREATE TABLE new_departments 
   (department_id, department_name, location_id)
   AS SELECT department_id, department_name, location_id 
   FROM departments;