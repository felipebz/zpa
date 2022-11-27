-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/STATS_T_TEST_.html
SELECT AVG(prod_list_price) group_mean,
       STATS_T_TEST_ONE(prod_list_price, 60, 'STATISTIC') t_observed,
       STATS_T_TEST_ONE(prod_list_price, 60) two_sided_p_value
  FROM sh.products;