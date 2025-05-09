-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/sql-json-function-json_array.html
SELECT JSON [ SELECT JSON {'id'   : employee_id,
                           'name' : last_name,
                           'sal'  : salary}
                FROM employees
                WHERE salary > 12000
                ORDER BY salary ] by_salary;