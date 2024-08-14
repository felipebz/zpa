-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT deptno, ename FROM emp
ORDER BY sal DESC
FETCH FIRST 2 PARTITIONS BY deptno, 3 ROWS ONLY;