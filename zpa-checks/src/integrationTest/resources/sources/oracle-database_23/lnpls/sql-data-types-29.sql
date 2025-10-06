-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-data-types.html
CREATE TABLE PLS_VEC_TAB(
    v1 vector, 
    v2 vector(100), 
    v3 vector(*, INT8),
    v4 vector(100, INT8), 
    v5 vector(1024, BINARY),
    v6 vector(100, FLOAT32, DENSE),
    v7 vector(100, FLOAT32, SPARSE)
);
DECLARE
    vec0 vector;                 -- dimension and format are flexible, storage format is DENSE
    vec1 PLS_VEC_TAB.v1%TYPE;    -- dimension and format are flexible, storage format is DENSE
    vec2 PLS_VEC_TAB.v2%TYPE;    -- dimension is 100, format is flexible, storage format is DENSE
    vec3 PLS_VEC_TAB.v3%TYPE;    -- dimension is flexible, format is INT8, storage format is DENSE
    vec4 PLS_VEC_TAB.v4%TYPE;    -- dimension is 100, format is INT8, storage format is DENSE
    vec5 PLS_VEC_TAB.v5%TYPE;    -- dimension is 1024, format is BINARY, storage format is DENSE
    vec6 PLS_VEC_TAB.v6%TYPE;    -- dimension is 100, format is FLOAT32, storage format is DENSE
    vec7 PLS_VEC_TAB.v7%TYPE;    -- dimension is 100, format is FLOAT32, storage format is SPARSE

    vec_0 vec0%TYPE;    -- dimension and format are flexible, storage format is DENSE
    vec_1 vec1%TYPE;    -- dimension and format are flexible, storage format is DENSE
    vec_2 vec2%TYPE;    -- dimension is 100, format is flexible, storage format is DENSE
    vec_3 vec3%TYPE;    -- dimension is flexible, format is INT8, storage format is DENSE
    vec_4 vec4%TYPE;    -- dimension is 100, format is INT8, storage format is DENSE
    vec_5 vec5%TYPE;    -- dimension is 1024, format is BINARY, storage format is DENSE
    vec_6 vec6%TYPE;    -- dimension is 100, format is FLOAT32, storage format is DENSE
    vec_7 vec7%TYPE;    -- dimension is 100, format is FLOAT32, storage format is SPARSE
BEGIN
    NULL;
END;
/