-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/TO_BINARY_DOUBLE.html
SELECT DUMP(dec_num) "Decimal",
   DUMP(bin_double) "Double"
   FROM float_point_demo;