-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/constraint.html
CREATE TABLE dept_20
   (employee_id     NUMBER(4) PRIMARY KEY, 
    last_name       VARCHAR2(10), 
    job_id          VARCHAR2(9), 
    manager_id      NUMBER(4), 
    salary          NUMBER(7,2), 
    commission_pct  NUMBER(7,2), 
    department_id   NUMBER(2),
    CONSTRAINT check_sal CHECK (salary * commission_pct <= 5000));