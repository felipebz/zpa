-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-vector-index.html
CREATE VECTOR INDEX galaxies_ivf_idx ON galaxies (embedding) ORGANIZATION NEIGHBOR PARTITIONS
DISTANCE COSINE
WITH TARGET ACCURACY 95;