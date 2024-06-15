-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/LAST_DAY.html
SELECT last_name, hire_date,
       TO_CHAR(ADD_MONTHS(LAST_DAY(hire_date), 5)) "Eval Date"
  FROM employees
  ORDER BY last_name, hire_date;