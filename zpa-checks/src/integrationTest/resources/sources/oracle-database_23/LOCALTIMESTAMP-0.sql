-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/LOCALTIMESTAMP.html
ALTER SESSION SET TIME_ZONE = '-5:00';
SELECT CURRENT_TIMESTAMP, LOCALTIMESTAMP FROM DUAL;