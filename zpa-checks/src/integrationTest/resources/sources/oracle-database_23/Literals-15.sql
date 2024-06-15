-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Literals.html
INSERT INTO my_table VALUES (1, SYSDATE);
INSERT INTO my_table VALUES (2, TRUNC(SYSDATE));

SELECT *
  FROM my_table;