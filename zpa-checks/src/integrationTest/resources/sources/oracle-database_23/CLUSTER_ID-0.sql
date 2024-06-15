-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CLUSTER_ID.html
SELECT CLUSTER_ID(km_sh_clus_sample USING *) AS clus, COUNT(*) AS cnt 
  FROM mining_data_apply_v
  GROUP BY CLUSTER_ID(km_sh_clus_sample USING *)
  ORDER BY cnt DESC;