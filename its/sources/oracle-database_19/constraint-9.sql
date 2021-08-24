CREATE TABLE dept_20 
   (employee_id     NUMBER(4) PRIMARY KEY, 
    last_name       VARCHAR2(10), 
    job_id          VARCHAR2(9), 
    manager_id      NUMBER(4) CONSTRAINT fk_mgr
                    REFERENCES employees ON DELETE SET NULL, 
    hire_date       DATE, 
    salary          NUMBER(7,2), 
    commission_pct  NUMBER(7,2), 
    department_id   NUMBER(2)   CONSTRAINT fk_deptno 
                    REFERENCES departments(department_id) 
                    ON DELETE CASCADE );