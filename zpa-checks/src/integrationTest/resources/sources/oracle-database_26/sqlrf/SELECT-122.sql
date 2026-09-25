-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html
  SELECT ENAME, SAL, DNAME, LOC
    FROM EMP, DEPT
    WHERE EMP.DEPTNO = DEPT.DEPTNO
    WINDOW W AS (PARTITION BY LOC)
    QUALIFY AVG(SAL) OVER W > 2000
    ORDER BY ENAME;