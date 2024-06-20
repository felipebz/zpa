-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-CLUSTER.html
CREATE TABLE empl (  
   emplno NUMBER(5) PRIMARY KEY,  
   emplname VARCHAR2(15) NOT NULL,  
   deptno NUMBER(3) REFERENCES dept)  
   CLUSTER emp_dept (deptno);