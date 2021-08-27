-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/GRANT.html
CREATE TABLE dependent 
   (dependno   NUMBER, 
    dependname VARCHAR2(10), 
    employee   NUMBER 
   CONSTRAINT in_emp REFERENCES hr.employees(employee_id) );