-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_CHAR-character.html
CREATE TABLE empl_temp 
  ( 
     employee_id NUMBER(6), 
     first_name  VARCHAR2(20), 
     last_name   VARCHAR2(25), 
     email       VARCHAR2(25), 
     hire_date   DATE DEFAULT SYSDATE, 
     job_id      VARCHAR2(10), 
     clob_column CLOB 
  );
INSERT INTO empl_temp
VALUES(111,'John','Doe','example.com','10-JAN-2015','1001','Experienced Employee');
INSERT INTO empl_temp
VALUES(112,'John','Smith','example.com','12-JAN-2015','1002','Junior Employee');
INSERT INTO empl_temp
VALUES(113,'Johnnie','Smith','example.com','12-JAN-2014','1002','Mid-Career Employee');
INSERT INTO empl_temp
VALUES(115,'Jane','Doe','example.com','15-JAN-2015','1005','Executive Employee');