-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/trigger-design-guidelines.html
CREATE OR REPLACE TRIGGER my_trigger
  AFTER CREATE ON DATABASE
BEGIN
  NULL;
END;
/