-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/overview-polymorphic-table-functions.html
CREATE FUNCTION to_doc(
                 tab  TABLE, 
       			    cols  COLUMNS DEFAULT NULL) 
       			    RETURN TABLE
    PIPELINED ROW POLYMORPHIC USING to_doc_p;