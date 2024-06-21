-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/CREATE-TYPE-statement.html
CREATE TYPE person_t AS OBJECT (name VARCHAR2(100), ssn NUMBER) 
   NOT FINAL;
/
CREATE TYPE employee_t UNDER person_t 
   (department_id NUMBER, salary NUMBER) NOT FINAL;
/
CREATE TYPE part_time_emp_t UNDER employee_t (num_hrs NUMBER);
/