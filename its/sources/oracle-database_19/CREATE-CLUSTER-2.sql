-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-CLUSTER.html
CREATE TABLE dept_10
   CLUSTER personnel (department_id)
   AS SELECT * FROM employees WHERE department_id = 10;

CREATE TABLE dept_20
   CLUSTER personnel (department_id)
   AS SELECT * FROM employees WHERE department_id = 20;