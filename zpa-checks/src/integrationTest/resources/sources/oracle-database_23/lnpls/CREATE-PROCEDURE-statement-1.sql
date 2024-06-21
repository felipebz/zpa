-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/CREATE-PROCEDURE-statement.html
CREATE PROCEDURE find_root
   ( x IN REAL ) 
   IS LANGUAGE C
      NAME c_find_root
      LIBRARY c_utils
      PARAMETERS ( x BY REFERENCE );