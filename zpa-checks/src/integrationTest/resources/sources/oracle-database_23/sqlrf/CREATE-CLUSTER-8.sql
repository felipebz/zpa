-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-CLUSTER.html
CREATE TABLE dept (  
   deptno NUMBER(3) PRIMARY KEY)  
   CLUSTER emp_dept (deptno);