-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/TO_BINARY_DOUBLE.html
CREATE TABLE float_point_demo
  (dec_num NUMBER(10,2), bin_double BINARY_DOUBLE, bin_float BINARY_FLOAT);

INSERT INTO float_point_demo
  VALUES (1234.56,1234.56,1234.56);

SELECT * FROM float_point_demo;