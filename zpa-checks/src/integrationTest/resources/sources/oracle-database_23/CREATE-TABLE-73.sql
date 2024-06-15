-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-TABLE.html
CREATE TABLE t2 (id NUMBER, doc XMLTYPE)
  XMLTYPE doc STORE AS TRANSPORTABLE BINARY XML;