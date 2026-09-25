-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-assertion.html
CREATE ASSERTION no_empty_departments CHECK
(ALL (SELECT d.deptno 
       FROM dept d) da 
 SATISFY 
  (EXISTS 
    (SELECT '' 
       FROM emp e 
       WHERE e.deptno = da.deptno)));