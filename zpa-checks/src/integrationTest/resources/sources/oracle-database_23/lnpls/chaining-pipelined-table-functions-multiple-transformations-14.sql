-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/chaining-pipelined-table-functions-multiple-transformations.html
SELECT * FROM TABLE (
  refcur_pkg.g_trans (
    CURSOR (SELECT * FROM employees WHERE department_id = 60),
    CURSOR (SELECT * FROM departments WHERE department_id = 60)
  )
);