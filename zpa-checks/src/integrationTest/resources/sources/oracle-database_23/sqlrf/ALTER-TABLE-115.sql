-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
SELECT t1.*, anydata.getTypeName(t1.x) typename FROM t1;