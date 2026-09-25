-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html
  SELECT ENAME, SAL, DNAME, LOC
    FROM EMP, DEPT
    WHERE EMP.DEPTNO = DEPT.DEPTNO
    QUALIFY AVG(SAL) OVER (PARTITION BY LOC) > 2000
    ORDER BY ENAME;