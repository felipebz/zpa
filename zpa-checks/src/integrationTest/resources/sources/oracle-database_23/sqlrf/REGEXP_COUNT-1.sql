-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REGEXP_COUNT.html
SELECT REGEXP_COUNT('123123123123', '123', 3, 'i') COUNT FROM DUAL;