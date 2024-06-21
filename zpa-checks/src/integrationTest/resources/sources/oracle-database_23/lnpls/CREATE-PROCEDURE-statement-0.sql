-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/CREATE-PROCEDURE-statement.html
CREATE PROCEDURE IF NOT EXISTS remove_emp (employee_id NUMBER) AS
   tot_emps NUMBER;
   BEGIN
      DELETE FROM employees
      WHERE employees.employee_id = remove_emp.employee_id;
   tot_emps := tot_emps - 1;
   END;
/