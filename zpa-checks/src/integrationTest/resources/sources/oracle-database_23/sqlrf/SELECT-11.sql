-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT docID FROM vec_table
ORDER BY VECTOR_DISTANCE(data, :query_vec)
FETCH APPROX FIRST 20 ROWS ONLY;