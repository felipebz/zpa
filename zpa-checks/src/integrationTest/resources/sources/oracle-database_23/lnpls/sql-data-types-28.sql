-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-data-types.html
DECLARE
    vec0 vector;                 -- dimension and format are flexible
    vec1 PLS_VEC_TAB.v1%TYPE;    -- dimension and format are flexible
    vec2 PLS_VEC_TAB.v2%TYPE;    -- dimension is 100, format is flexible
    vec3 PLS_VEC_TAB.v3%TYPE;    -- dimension is flexible, format is INT8
    vec4 PLS_VEC_TAB.v4%TYPE;    -- dimension is 100, format is INT8
    vec5 PLS_VEC_TAB.v5%TYPE;    -- dimension is 1024, format is BINARY

    vec_0 vec0%TYPE;    -- dimension and format are flexible
    vec_1 vec1%TYPE;    -- dimension and format are flexible
    vec_2 vec2%TYPE;    -- dimension is 100, format is flexible
    vec_3 vec3%TYPE;    -- dimension is flexible, format is INT8
    vec_4 vec4%TYPE;    -- dimension is 100, format is INT8
    vec_5 vec5%TYPE;    -- dimension is 1024, format is BINARY
BEGIN
    NULL;
END;
/