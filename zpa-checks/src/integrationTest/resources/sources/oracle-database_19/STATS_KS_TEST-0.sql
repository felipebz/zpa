-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/STATS_KS_TEST.html
SELECT stats_ks_test(cust_gender, amount_sold, 'STATISTIC') ks_statistic,
       stats_ks_test(cust_gender, amount_sold) p_value
  FROM sh.customers c, sh.sales s
  WHERE c.cust_id = s.cust_id;