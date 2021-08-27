-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/TO_BINARY_FLOAT.html
SELECT dec_num, TO_BINARY_FLOAT(dec_num)
  FROM float_point_demo;