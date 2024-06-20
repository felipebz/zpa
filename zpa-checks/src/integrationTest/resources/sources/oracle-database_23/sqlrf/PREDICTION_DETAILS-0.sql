-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/PREDICTION_DETAILS.html
SELECT PREDICTION_DETAILS(svmr_sh_regr_sample, null, 3 USING *) prediction_details
    FROM mining_data_apply_v
    WHERE cust_id = 100001;