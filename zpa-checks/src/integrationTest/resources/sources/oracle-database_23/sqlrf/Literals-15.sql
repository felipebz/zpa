-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Literals.html
INSERT INTO my_table VALUES (1, SYSDATE);
INSERT INTO my_table VALUES (2, TRUNC(SYSDATE));
SELECT *
  FROM my_table;
SELECT *
  FROM my_table
  WHERE datecol > TO_DATE('02-OCT-02', 'DD-MON-YY');
SELECT *
  FROM my_table
  WHERE datecol = TO_DATE('03-OCT-02','DD-MON-YY');