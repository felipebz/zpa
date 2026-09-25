-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/INSERT.html
  INSERT INTO employees SET
   (employee_id = 210, last_name = 'Smith', email = 'ASMITH', hire_date = SYSDATE, job_id = 'AD_ASST'),
   (employee_id = 211, last_name = 'Roddick', email = 'ARODDICK', hire_date = SYSDATE, job_id = 'IT_PROG');