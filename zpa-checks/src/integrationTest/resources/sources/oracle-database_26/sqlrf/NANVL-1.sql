-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/NANVL.html
SELECT bin_float, NANVL(bin_float,0)
  FROM float_point_demo;