-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/bulk-sql-and-bulk-binding.html
BEGIN
  FORALL i IN :lower..:upper
    DELETE FROM employees
    WHERE department_id = :depts(i);
END;
/