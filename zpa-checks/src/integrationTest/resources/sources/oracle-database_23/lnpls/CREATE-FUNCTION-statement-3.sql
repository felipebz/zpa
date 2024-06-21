-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/CREATE-FUNCTION-statement.html
SELECT SecondMax(salary) "SecondMax", department_id
      FROM employees
      GROUP BY department_id
      HAVING SecondMax(salary) > 9000
      ORDER BY "SecondMax", department_id;