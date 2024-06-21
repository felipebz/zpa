-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-data-types.html
dist := COSINE_DISTANCE(v1, v2);
dist := VECTOR_DISTANCE(v1, v2, COSINE);
SELECT VECTOR_DISTANCE(v1, v2, COSINE) INTO dist;
SELECT v1 <=> v2 INTO dist;