-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Using-Extensible-Indexing.html
CREATE OR REPLACE OPERATOR position_between
   BINDING (NUMBER, NUMBER, NUMBER) RETURN NUMBER 
   WITH INDEX CONTEXT, SCAN CONTEXT position_im
   USING function_for_position_between;