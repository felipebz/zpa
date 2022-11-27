-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CAST.html
SELECT CAST('January 15, 1989, 11:00 A.M.'
       AS DATE
       DEFAULT NULL ON CONVERSION ERROR,
       'Month dd, YYYY, HH:MI A.M.')
  FROM DUAL;