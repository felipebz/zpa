-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/dml-triggers.html
INSERT INTO TABLE (
  SELECT d.emplist 
  FROM dept_view d
  WHERE department_id = 10
)
VALUES (1001, 'Glenn', 'AC_MGR', 10000);