-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/SQL_MACRO-clause.html
CREATE FUNCTION budget(job VARCHAR2) RETURN VARCHAR2 SQL_MACRO IS
BEGIN
   RETURN q'{SELECT deptno, SUM(sal) budget 
             FROM scott.emp
             WHERE job = budget.job
             GROUP BY deptno}';
END;