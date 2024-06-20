-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT * FROM employees e, (SELECT * FROM departments d
                            WHERE e.department_id = d.department_id);