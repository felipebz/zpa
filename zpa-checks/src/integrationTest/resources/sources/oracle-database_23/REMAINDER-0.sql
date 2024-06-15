-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REMAINDER.html
SELECT bin_float, bin_double, REMAINDER(bin_float, bin_double)
  FROM float_point_demo;