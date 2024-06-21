-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-optimization-and-tuning.html
CREATE FUNCTION to_doc(
                 tab  TABLE, 
       			    cols  COLUMNS DEFAULT NULL) 
       			    RETURN TABLE
    PIPELINED ROW POLYMORPHIC USING to_doc_p;