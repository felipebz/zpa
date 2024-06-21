-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-optimization-and-tuning.html
CREATE FUNCTION skip_col_by_name(tab TABLE, 
                                 col COLUMNS)
                  RETURN TABLE PIPELINED ROW POLYMORPHIC USING skip_col_pkg;