-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-assertion.html
CREATE ASSERTION no_empty_departments CHECK  
(NOT EXISTS  
  (SELECT 'an empty department' 
     FROM dept d 
     WHERE NOT EXISTS 
                   (SELECT 'an employee in the department' 
                      FROM emp e 
                      WHERE e.deptno = d.deptno)));