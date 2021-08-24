CREATE TABLE dept_20 
   (employee_id     NUMBER(4), 
    last_name       VARCHAR2(10), 
    job_id          VARCHAR2(9), 
    manager_id      NUMBER(4), 
    hire_date       DATE, 
    salary          NUMBER(7,2), 
    commission_pct  NUMBER(7,2), 
    department_id   CONSTRAINT fk_deptno 
                    REFERENCES departments(department_id) );