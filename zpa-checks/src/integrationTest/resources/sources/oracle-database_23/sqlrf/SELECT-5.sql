-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT
      ename, mgr,
      FIRST_VALUE(sal) OVER w AS "first",
      LAST_VALUE(sal) OVER w AS "last",
      NTH_VALUE(sal, 2) OVER w AS "second",
      NTH_VALUE(sal, 4) OVER w AS "fourth"
   FROM emp
   WINDOW w AS (PARTITION BY deptno ORDER BY sal ROWS UNBOUNDED PRECEDING);