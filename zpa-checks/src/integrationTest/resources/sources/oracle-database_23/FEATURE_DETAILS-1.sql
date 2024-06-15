-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/FEATURE_DETAILS.html
SELECT feature_id, value
  FROM (
     SELECT cust_id, feature_set(INTO 6 USING *) OVER () fset
        FROM mining_data_apply_v),
  TABLE (fset)
  WHERE cust_id = 100001
  ORDER BY feature_id;