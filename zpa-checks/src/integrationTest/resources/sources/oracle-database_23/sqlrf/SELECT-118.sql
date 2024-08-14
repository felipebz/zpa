-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
CREATE TABLE chunk_table ( 
  doc_id NUMBER, 
  chunk_id NUMBER, 
  data_vec VECTOR
 );