-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Comments.html
CREATE VIEW v AS
  SELECT e.last_name, e.department_id, d.location_id
  FROM employees e, departments d
  WHERE e.department_id = d.department_id;
CREATE TABLE t AS
  SELECT * from employees
  WHERE employee_id < 200;