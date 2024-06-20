-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
CREATE TYPE dept_t AS OBJECT 
   (deptno NUMBER, dname VARCHAR2(20));
/
CREATE TABLE staff 
   (name   VARCHAR2(100), 
    salary NUMBER,
    dept   REF dept_t);