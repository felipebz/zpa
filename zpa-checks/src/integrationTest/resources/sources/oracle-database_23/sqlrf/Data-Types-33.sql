-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Data-Types.html
CREATE TABLE my_vect_tab (
     v1 VECTOR(3, FLOAT32),
     v2 VECTOR(2, FLOAT64),
     v3 VECTOR(1, INT8),
     v4 VECTOR(1, *),
     v5 VECTOR(*, FLOAT32),
     v6 VECTOR(*, *),
     v7 VECTOR
   );
DESC my_vect_tab;