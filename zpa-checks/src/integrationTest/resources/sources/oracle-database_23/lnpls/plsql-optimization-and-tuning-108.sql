-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-optimization-and-tuning.html
CREATE FUNCTION skip_col_by_type(tab TABLE, 
                                 type_name VARCHAR2,
                                 flip VARCHAR2 DEFAULT 'False')
                  RETURN TABLE PIPELINED ROW POLYMORPHIC USING skip_col_pkg;