-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-TABLE.html
CREATE OR REPLACE TYPE salesrep_typ AS OBJECT
  ( repId NUMBER,
    repName VARCHAR2(64));

CREATE TABLE salesreps OF salesrep_typ;