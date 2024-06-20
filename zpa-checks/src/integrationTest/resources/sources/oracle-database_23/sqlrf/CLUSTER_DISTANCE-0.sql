-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CLUSTER_DISTANCE.html
SELECT cust_id
  FROM (
    SELECT cust_id,
           rank() over
             (order by CLUSTER_DISTANCE(km_sh_clus_sample USING *) desc) rnk
      FROM mining_data_apply_v)
  WHERE rnk <= 11
  ORDER BY rnk;