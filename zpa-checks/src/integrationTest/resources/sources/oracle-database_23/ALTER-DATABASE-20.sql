-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-DATABASE.html
ALTER DATABASE TEMPFILE 'temp02.dbf' OFFLINE;

ALTER DATABASE RENAME FILE 'temp02.dbf' TO 'temp03.dbf';