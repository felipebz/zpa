-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-TABLE.html
CREATE DOMAIN dm1 AS
 (ann AS NUMBER NOT NULL ,
  bnnpos AS NUMBER NOT NULL CONSTRAINT CHECK (bnnpos > 0),
  c AS VARCHAR2(10) DEFAULT 'abc',
  ddon AS NUMBER DEFAULT ON NULL 10)  
  CONSTRAINT CHECK (ann+ddon < = 100)     
  CONSTRAINT CHECK (length(c) > bnnpos);