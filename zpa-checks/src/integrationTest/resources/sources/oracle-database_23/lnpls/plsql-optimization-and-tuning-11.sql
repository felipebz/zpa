-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-optimization-and-tuning.html
DELETE FROM employees_temp WHERE department_id = 10;
DELETE FROM employees_temp WHERE department_id = 30;
DELETE FROM employees_temp WHERE department_id = 70;