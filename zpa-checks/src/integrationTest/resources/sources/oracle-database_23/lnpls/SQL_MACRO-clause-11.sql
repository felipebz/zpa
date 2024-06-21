-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/SQL_MACRO-clause.html
VARIABLE surname VARCHAR2(100)

EXEC :surname := 'ellison'

WITH e AS (SELECT emp.*, :surname lname FROM emp WHERE deptno IN (10,20))
SELECT deptno,
       emp_doc(first_name => ename, last_name => lname, hire_date => hiredate) doc
FROM e
ORDER BY ename;