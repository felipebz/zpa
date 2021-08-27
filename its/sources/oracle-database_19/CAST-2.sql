-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CAST.html
SELECT CAST(200
       AS NUMBER
       DEFAULT 0 ON CONVERSION ERROR)
  FROM DUAL;