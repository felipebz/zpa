-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/TRIM.html
SELECT employee_id,
      TO_CHAR(TRIM(LEADING 0 FROM hire_date))
      FROM employees
      WHERE department_id = 60
      ORDER BY employee_id;