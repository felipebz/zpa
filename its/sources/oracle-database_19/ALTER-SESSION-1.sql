-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-SESSION.html
SELECT SYS_CONTEXT('USERENV', 'CON_NAME') FROM DUAL;