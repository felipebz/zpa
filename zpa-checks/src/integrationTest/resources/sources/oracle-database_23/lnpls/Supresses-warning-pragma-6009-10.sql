-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/Supresses-warning-pragma-6009.html
CREATE OR REPLACE TYPE newid AUTHID DEFINER
AS OBJECT(
  ID1 NUMBER,
  MEMBER PROCEDURE incr,
  MEMBER PROCEDURE log_error,
  PRAGMA SUPPRESSES_WARNING_6009(log_error)
);
/