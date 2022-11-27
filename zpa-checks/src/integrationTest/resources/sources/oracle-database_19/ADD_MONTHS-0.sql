-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ADD_MONTHS.html
SELECT TO_CHAR(ADD_MONTHS(hire_date, 1), 'DD-MON-YYYY') "Next month"
  FROM employees 
  WHERE last_name = 'Baer';