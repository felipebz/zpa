-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SYS_EXTRACT_UTC.html
SELECT SYS_EXTRACT_UTC(TIMESTAMP '2000-03-28 11:30:00.00 -08:00')
   FROM DUAL;