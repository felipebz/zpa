-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-SESSION.html
SELECT SYS_CONTEXT('USERENV', 'SESSION_DEFAULT_COLLATION') FROM DUAL;