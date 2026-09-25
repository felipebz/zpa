-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SYS_CONTEXT.html
CONNECT OE
SELECT SYS_CONTEXT ('USERENV', 'SESSION_USER') 
   FROM DUAL;