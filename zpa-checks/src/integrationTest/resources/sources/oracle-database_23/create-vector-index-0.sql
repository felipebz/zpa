-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-vector-index.html
CREATE VECTOR INDEX galaxies_hnsw_idx ON galaxies (embedding) ORGANIZATION INMEMORY NEIGHBOR GRAPH
DISTANCE COSINE
WITH TARGET ACCURACY 95;