-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/UPDATE.html
UPDATE employees
  SET job_id ='SA_MAN', salary = salary + 1000, department_id = 140
  WHERE last_name = 'Jones'
  RETURNING salary*0.25, last_name, department_id
    INTO :bnd1, :bnd2, :bnd3;