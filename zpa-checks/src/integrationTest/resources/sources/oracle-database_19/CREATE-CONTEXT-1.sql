-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-CONTEXT.html
CREATE VIEW hr_org_secure_view AS
   SELECT * FROM employees
   WHERE department_id = SYS_CONTEXT('hr_context', 'department_id');