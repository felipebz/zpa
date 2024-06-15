-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
CREATE DOMAIN phone_number as VARCHAR2(12)  
  CONSTRAINT CHECK (phone_number not like '%[0-9]%')
  NOT NULL;