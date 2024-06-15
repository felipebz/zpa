-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Joins.html
SELECT * FROM A, B, D
  WHERE A.c1 = B.c2(+) and D.c3 = B.c4(+);