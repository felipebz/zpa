-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Literals.html
SELECT *
  FROM my_table
  WHERE TRUNC(datecol) = DATE '2002-10-03';