CREATE TYPE dept_t AS OBJECT 
   (deptno NUMBER, dname VARCHAR2(20));
/

CREATE TABLE staff 
   (name   VARCHAR2(100), 
    salary NUMBER,
    dept   REF dept_t);