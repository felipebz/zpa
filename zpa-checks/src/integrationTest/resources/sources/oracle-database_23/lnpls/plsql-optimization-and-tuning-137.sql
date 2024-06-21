-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-optimization-and-tuning.html
  SPOOL pre_update_invalid.log

SELECT o.OWNER, o.OBJECT_NAME, o.OBJECT_TYPE 
FROM DBA_OBJECTS o, DBA_PLSQL_OBJECT_SETTINGS s 
WHERE o.OBJECT_NAME = s.NAME AND o.STATUS='INVALID';