-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/generation.html
SELECT json_arrayagg(department_name)FROM departments;