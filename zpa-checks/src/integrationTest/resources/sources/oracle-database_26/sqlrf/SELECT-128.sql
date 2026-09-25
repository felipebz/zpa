-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html
  SELECT E1.ENAME, E1.SAL, AVG(E1.SAL) OVER W1 AS AVG_SAL1
    FROM EMP E1
    WINDOW W1 AS (PARTITION BY E1.MGR)
    QUALIFY AVG_SAL1 IN (
                          SELECT AVG(E2.SAL) OVER W2 AS AVG_SAL2
                          FROM EMP E2
                          WINDOW W2 AS (PARTITION BY MGR)
                          QUALIFY AVG_SAL2 = AVG(E.SAL) OVER W2
                        );