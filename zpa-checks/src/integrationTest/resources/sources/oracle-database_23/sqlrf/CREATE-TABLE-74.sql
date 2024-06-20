-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-TABLE.html
ALTER TABLE t3 ADD (doc XMLTYPE)
  XMLTYPE doc STORE AS TRANSPORTABLE BINARY XML;