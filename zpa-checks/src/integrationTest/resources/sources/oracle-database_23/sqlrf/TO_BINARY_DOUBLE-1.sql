-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_BINARY_DOUBLE.html
SELECT dec_num, TO_BINARY_DOUBLE(dec_num)
  FROM float_point_demo;