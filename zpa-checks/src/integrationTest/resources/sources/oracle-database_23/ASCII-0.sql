-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ASCII.html
SELECT last_name
  FROM employees
  WHERE ASCII(SUBSTR(last_name, 1, 1)) = 76
  ORDER BY last_name;