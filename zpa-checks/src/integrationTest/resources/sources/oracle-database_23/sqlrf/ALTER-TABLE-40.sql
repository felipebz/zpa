-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
CREATE TABLE JOBS_Temp AS SELECT * FROM HR.JOBS;
SELECT * FROM JOBS_Temp WHERE MAX_SALARY > 20000;