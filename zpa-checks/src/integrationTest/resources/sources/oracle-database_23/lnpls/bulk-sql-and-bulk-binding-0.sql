-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/bulk-sql-and-bulk-binding.html
DELETE FROM employees_temp WHERE department_id = 10;
DELETE FROM employees_temp WHERE department_id = 30;
DELETE FROM employees_temp WHERE department_id = 70;