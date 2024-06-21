-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-optimization-and-tuning.html
CREATE PACKAGE skip_col_pkg AS

  -- OVERLOAD 1: Skip by name --
  FUNCTION skip_col(tab TABLE, 
                    col COLUMNS)
           RETURN TABLE PIPELINED ROW POLYMORPHIC USING skip_col_pkg;

  FUNCTION describe(tab IN OUT DBMS_TF.TABLE_T, 
                    col        DBMS_TF.COLUMNS_T)
           RETURN DBMS_TF.DESCRIBE_T;

  -- OVERLOAD 2: Skip by type --
  FUNCTION skip_col(tab       TABLE, 
                    type_name VARCHAR2,
                    flip      VARCHAR2 DEFAULT 'False') 
           RETURN TABLE PIPELINED ROW POLYMORPHIC USING skip_col_pkg;

  FUNCTION describe(tab       IN OUT DBMS_TF.TABLE_T, 
                    type_name        VARCHAR2, 
                    flip             VARCHAR2 DEFAULT 'False') 
           RETURN DBMS_TF.DESCRIBE_T;

END skip_col_pkg;