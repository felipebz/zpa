-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/SQL_MACRO-clause.html
SELECT department_id,
            emp_doc(first_name => e.first_name, hire_date => e.hire_date) doc
FROM hr.employees e
WHERE department_id = 30
ORDER BY last_name;