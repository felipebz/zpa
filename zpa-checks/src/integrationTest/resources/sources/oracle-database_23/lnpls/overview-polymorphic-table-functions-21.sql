-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/overview-polymorphic-table-functions.html
SELECT deptno, ename, document 
FROM   to_doc(scott.emp, COLUMNS(empno,job,mgr,hiredate,sal,comm))
WHERE  deptno IN (10, 30) 
ORDER BY 1, 2;