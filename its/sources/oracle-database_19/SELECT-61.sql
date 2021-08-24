CREATE TABLE new_departments 
   (department_id, department_name, location_id)
   AS SELECT department_id, department_name, location_id 
   FROM departments;