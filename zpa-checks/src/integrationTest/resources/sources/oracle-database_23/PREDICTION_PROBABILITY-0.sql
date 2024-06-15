-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/PREDICTION_PROBABILITY.html
SELECT cust_id FROM (
   SELECT cust_id
   FROM mining_data_apply_v
   WHERE country_name = 'Italy'
   ORDER BY PREDICTION_PROBABILITY(DT_SH_Clas_sample, 1 USING *)
      DESC, cust_id)
   WHERE rownum < 11;