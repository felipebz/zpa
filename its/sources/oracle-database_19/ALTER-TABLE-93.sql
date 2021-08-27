-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-TABLE.html
CREATE TYPE emp_t AS OBJECT (eno number, ename char(31)); 
CREATE TYPE emps_t AS TABLE OF REF emp_t; 
CREATE TABLE emptab OF emp_t; 
CREATE TABLE dept (dno NUMBER, employees emps_t) 
   NESTED TABLE employees STORE AS deptemps; 
ALTER TABLE deptemps ADD (SCOPE FOR (COLUMN_VALUE) IS emptab);