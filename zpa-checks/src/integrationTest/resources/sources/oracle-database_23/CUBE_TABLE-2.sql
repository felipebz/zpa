-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CUBE_TABLE.html
SELECT sales, units, cost, time, customer, product, channel
  FROM TABLE(CUBE_TABLE('global.units_cube HIERARCHY customer market HIERARCHY time calendar'))
  WHERE rownum < 20;