-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/overview-polymorphic-table-functions.html
CREATE FUNCTION skip_col_by_name(tab TABLE, 
                                 col COLUMNS)
                  RETURN TABLE PIPELINED ROW POLYMORPHIC USING skip_col_pkg;