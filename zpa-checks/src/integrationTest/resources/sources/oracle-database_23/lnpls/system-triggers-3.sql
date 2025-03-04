-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/system-triggers.html
CREATE OR REPLACE TRIGGER t
  INSTEAD OF CREATE ON SCHEMA
  BEGIN
    EXECUTE IMMEDIATE 'CREATE TABLE T (n NUMBER, m NUMBER)';
  END;
/