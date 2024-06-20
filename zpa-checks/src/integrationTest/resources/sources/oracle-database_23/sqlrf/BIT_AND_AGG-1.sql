-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/BIT_AND_AGG.html
SELECT bit_and_agg(bits) 
  FROM (SELECT '011' num, bin_to_num(0,1,1) bits FROM dual
        UNION ALL SELECT '101' num, bin_to_num(1,0,1) bits FROM dual);