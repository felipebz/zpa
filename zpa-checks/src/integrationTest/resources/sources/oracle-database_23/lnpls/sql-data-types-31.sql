-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-data-types.html
SELECT VECTOR_DISTANCE(v1, v2, COSINE) INTO dist;
SELECT v1 <=> v2 INTO dist;